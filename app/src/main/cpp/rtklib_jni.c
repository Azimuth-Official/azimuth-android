/*
 * Azimuth RTKLIB JNI bridge — Phase 3c data plumbing.
 *
 * Provides native RTK positioning via RTKLIB demo5.  Accepts raw RTCM bytes
 * for base observations and ephemeris, and packed Android GnssMeasurement
 * arrays for rover observations.
 *
 * Memory: azimuth_ctx_t is heap-allocated per init, freed per nativeFree.
 * Each internal rtcm_t owns its nav_t arrays.  No shallow copies.
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <stdint.h>
#include <android/log.h>

#include "azimuth_ctx.h"

/*
 * Stubs for RTCM v2 decoder and RTCM v3 encoder — referenced by rtcm.c but
 * not needed for Azimuth (we only use input_rtcm3 for decoding, never
 * input_rtcm2 or gen_rtcm3).  Source files rtcm2.c and rtcm3e.c are not
 * vendored.
 */
int decode_rtcm2(rtcm_t *rtcm) { return 0; }
int encode_rtcm3(rtcm_t *rtcm, int type, int subtype, int sync) { return 0; }

#define LOG_TAG "RtklibJNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

/* JNI package prefix */
#define JNI_PREFIX Java_day_azimuth_observer_service_ntrip_RtklibNative_

/* ---- helpers ------------------------------------------------------------ */

/* Android constellationType → RTKLIB SYS_* */
static int android_const_to_sys(int c) {
    switch (c) {
        case 1: return SYS_GPS;
        case 2: return SYS_SBS;
        case 3: return SYS_GLO;
        case 4: return SYS_QZS;
        case 5: return SYS_CMP;
        case 6: return SYS_GAL;
        default: return 0;
    }
}

/*
 * Map Android carrier frequency to RTKLIB observation code and frequency
 * index.  Returns 1 on success, 0 if unmapped (measurement should be
 * skipped).
 */
static int map_freq_to_code(int sys, double freq_hz,
                            uint8_t *code, int *freq_idx)
{
    double f = freq_hz;
    if (sys == SYS_GPS) {
        if (fabs(f - 1575.42e6) < 10e6) { *code = CODE_L1C; *freq_idx = 0; return 1; }
        if (fabs(f - 1227.60e6) < 10e6) { *code = CODE_L2X; *freq_idx = 1; return 1; }
        if (fabs(f - 1176.45e6) < 10e6) { *code = CODE_L5X; *freq_idx = 2; return 1; }
    } else if (sys == SYS_GAL) {
        if (fabs(f - 1575.42e6) < 10e6) { *code = CODE_L1C; *freq_idx = 0; return 1; }
        if (fabs(f - 1176.45e6) < 10e6) { *code = CODE_L5X; *freq_idx = 2; return 1; }
        if (fabs(f - 1207.14e6) < 10e6) { *code = CODE_L7X; *freq_idx = 1; return 1; }
    } else if (sys == SYS_CMP) {
        if (fabs(f - 1561.098e6) < 10e6) { *code = CODE_L2I; *freq_idx = 0; return 1; }
        if (fabs(f - 1575.42e6)  < 10e6) { *code = CODE_L1X; *freq_idx = 0; return 1; }
        if (fabs(f - 1207.14e6)  < 10e6) { *code = CODE_L7I; *freq_idx = 1; return 1; }
        if (fabs(f - 1176.45e6)  < 10e6) { *code = CODE_L5X; *freq_idx = 2; return 1; }
    } else if (sys == SYS_GLO) {
        /* GLONASS FDMA — broad band match; exact freq via sat2freq() later */
        if (f > 1590e6 && f < 1620e6) { *code = CODE_L1C; *freq_idx = 0; return 1; }
        if (f > 1235e6 && f < 1260e6) { *code = CODE_L2C; *freq_idx = 1; return 1; }
    }
    return 0;
}

/* Build a no-fix JSON response with a reason string. */
static jstring no_fix_json(JNIEnv *env, const char *reason)
{
    char buf[512];
    snprintf(buf, sizeof(buf),
        "{\"hasFix\":false,\"lat\":0.0,\"lon\":0.0,\"alt\":0.0,"
        "\"accuracy\":null,\"nSat\":0,\"ratio\":0.0,"
        "\"message\":\"%s\"}", reason);
    return (*env)->NewStringUTF(env, buf);
}

/* ---- nativeInit --------------------------------------------------------- */

JNIEXPORT jlong JNICALL
Java_day_azimuth_observer_service_ntrip_RtklibNative_nativeInit(
    JNIEnv *env, jobject thiz)
{
    azimuth_ctx_t *ctx = (azimuth_ctx_t *)calloc(1, sizeof(azimuth_ctx_t));
    if (!ctx) {
        LOGW("nativeInit: calloc failed");
        return 0;
    }

    /* init_rtcm returns int: 1=ok, 0=fail (rtcm.c:66) */
    if (!init_rtcm(&ctx->rtcm_base)) {
        LOGW("nativeInit: init_rtcm(base) failed");
        free(ctx);
        return 0;
    }
    if (!init_rtcm(&ctx->rtcm_eph)) {
        LOGW("nativeInit: init_rtcm(eph) failed");
        free_rtcm(&ctx->rtcm_base); /* void (rtcm.c:131) */
        free(ctx);
        return 0;
    }

    /* Seed approximate time for RTCM time-ambiguity resolution */
    ctx->rtcm_base.time = utc2gpst(timeget());
    ctx->rtcm_eph.time  = utc2gpst(timeget());

    /* Processing options — conservative for mobile first run */
    ctx->opt = prcopt_default;
    ctx->opt.mode   = PMODE_KINEMA;
    ctx->opt.nf     = 2;                              /* dual-frequency */
    ctx->opt.navsys = SYS_GPS | SYS_GAL | SYS_CMP;   /* GLONASS staged off */
    ctx->opt.modear = 1;                               /* continuous AR (conservative) */
    ctx->opt.elmin  = 15.0 * D2R;                      /* 15 deg elevation mask */

    /* rtkinit returns void (rtkpos.c:2229) — no boolean check */
    rtkinit(&ctx->rtk, &ctx->opt);

    LOGI("nativeInit: azimuth_ctx_t initialized (Phase 3c)");
    return (jlong)(intptr_t)ctx;
}

/* ---- nativeFree --------------------------------------------------------- */

JNIEXPORT void JNICALL
Java_day_azimuth_observer_service_ntrip_RtklibNative_nativeFree(
    JNIEnv *env, jobject thiz, jlong handle)
{
    if (handle == 0) return;
    azimuth_ctx_t *ctx = (azimuth_ctx_t *)(intptr_t)handle;

    rtkfree(&ctx->rtk);          /* void */
    free_rtcm(&ctx->rtcm_base);  /* void */
    free_rtcm(&ctx->rtcm_eph);   /* void */
    free(ctx);

    LOGI("nativeFree: azimuth_ctx_t released");
}

/* ---- nativeFeedRtcm ----------------------------------------------------- */

JNIEXPORT jint JNICALL
Java_day_azimuth_observer_service_ntrip_RtklibNative_nativeFeedRtcm(
    JNIEnv *env, jobject thiz, jlong handle,
    jbyteArray data, jint length, jint streamKind)
{
    if (handle == 0 || length <= 0) return 0;
    azimuth_ctx_t *ctx = (azimuth_ctx_t *)(intptr_t)handle;

    jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
    if (!bytes) return 0;

    /* Route to decoder based on stream kind:
     *   STREAM_BASE_OBS  → rtcm_base (nearby base station observations)
     *   STREAM_EPHEMERIS → rtcm_eph  (ephemeris-only mount)
     *   STREAM_COMBINED  → rtcm_eph  (single mount with both obs + eph)
     */
    rtcm_t *rtcm;
    if (streamKind == STREAM_BASE_OBS) {
        rtcm = &ctx->rtcm_base;
    } else {
        rtcm = &ctx->rtcm_eph;
    }

    int result = 0;
    int i;

    for (i = 0; i < length; i++) {
        /* input_rtcm3 returns int: 0=incomplete, 1=obs, 2=eph, 5=sta, -1=err */
        int ret = input_rtcm3(rtcm, (uint8_t)bytes[i]);

        if (ret == 1) {
            /* Observation epoch complete — copy base obs with rcv=2 */
            if (rtcm->obs.n > 0) {
                int n = rtcm->obs.n;
                if (n > MAXOBS) n = MAXOBS;
                int j;
                for (j = 0; j < n; j++) {
                    ctx->base_obs[j] = rtcm->obs.data[j];
                    ctx->base_obs[j].rcv = 2;
                }
                ctx->base_nobs = n;
                ctx->base_time = rtcm->obs.data[0].time;
                result |= FEED_OBS_COMPLETE;
            }
        }
        if (ret == 2) {
            /* Ephemeris decoded — update constellation counters */
            int prn;
            int sys = satsys(rtcm->ephsat, &prn);
            if      (sys == SYS_GPS) ctx->eph_gps++;
            else if (sys == SYS_GLO) ctx->eph_glo++;
            else if (sys == SYS_GAL) ctx->eph_gal++;
            else if (sys == SYS_CMP) ctx->eph_bds++;
            result |= FEED_EPH_UPDATED;
        }
        if (ret == 5) {
            /* Station position from RTCM 1005/1006 */
            double *sp = rtcm->sta.pos;
            if (sp[0] != 0.0 || sp[1] != 0.0 || sp[2] != 0.0) {
                ctx->base_pos[0] = sp[0];
                ctx->base_pos[1] = sp[1];
                ctx->base_pos[2] = sp[2];
                ctx->has_base_pos = 1;
                result |= FEED_STATION_INFO;
            }
        }
    }

    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    return result;
}

/* ---- nativeProcessEpoch ------------------------------------------------- */

/*
 * Pack Android GnssMeasurement arrays into RTKLIB obsd_t, merge with base
 * observations, and call rtkpos().
 *
 * Contract: Kotlin caller MUST only invoke when hasFullBiasNanos()==true.
 * fullBiasNanos arrives as jlong (int64) — zero precision loss.
 */
JNIEXPORT jstring JNICALL
Java_day_azimuth_observer_service_ntrip_RtklibNative_nativeProcessEpoch(
    JNIEnv *env, jobject thiz, jlong handle,
    jlong timeNanos, jlong fullBiasNanos, jdouble biasNanos,
    jint nMeas,
    jintArray jsvids, jintArray jconstTypes, jintArray jstates,
    jlongArray jrecvSvTime, jdoubleArray jtimeOffset,
    jdoubleArray jcn0, jdoubleArray jcarrierFreq,
    jdoubleArray jprRate, jdoubleArray jadrMeters, jintArray jadrStates)
{
    if (handle == 0 || nMeas <= 0) {
        return no_fix_json(env, "no handle or no measurements");
    }
    azimuth_ctx_t *ctx = (azimuth_ctx_t *)(intptr_t)handle;

    /* Authoritative nav source: ephemeris decoder's nav_t (const pointer) */
    const nav_t *nav = &ctx->rtcm_eph.nav;

    /* Check ephemeris availability */
    if (nav->n <= 0 && nav->ng <= 0) {
        return no_fix_json(env, "no ephemeris available");
    }

    /* ---- Get JNI arrays ---- */
    jint    *svids       = (*env)->GetIntArrayElements(env, jsvids, NULL);
    jint    *constTypes  = (*env)->GetIntArrayElements(env, jconstTypes, NULL);
    jint    *states      = (*env)->GetIntArrayElements(env, jstates, NULL);
    jlong   *recvSvTime  = (*env)->GetLongArrayElements(env, jrecvSvTime, NULL);
    jdouble *timeOffset  = (*env)->GetDoubleArrayElements(env, jtimeOffset, NULL);
    jdouble *cn0         = (*env)->GetDoubleArrayElements(env, jcn0, NULL);
    jdouble *carrierFreq = (*env)->GetDoubleArrayElements(env, jcarrierFreq, NULL);
    jdouble *prRate      = (*env)->GetDoubleArrayElements(env, jprRate, NULL);
    jdouble *adrMeters   = (*env)->GetDoubleArrayElements(env, jadrMeters, NULL);
    jint    *adrStates   = (*env)->GetIntArrayElements(env, jadrStates, NULL);

    if (!svids || !constTypes || !states || !recvSvTime || !timeOffset ||
        !cn0 || !carrierFreq || !prRate || !adrMeters || !adrStates) {
        /* Release any that succeeded before failing */
        if (svids)       (*env)->ReleaseIntArrayElements(env, jsvids, svids, JNI_ABORT);
        if (constTypes)  (*env)->ReleaseIntArrayElements(env, jconstTypes, constTypes, JNI_ABORT);
        if (states)      (*env)->ReleaseIntArrayElements(env, jstates, states, JNI_ABORT);
        if (recvSvTime)  (*env)->ReleaseLongArrayElements(env, jrecvSvTime, recvSvTime, JNI_ABORT);
        if (timeOffset)  (*env)->ReleaseDoubleArrayElements(env, jtimeOffset, timeOffset, JNI_ABORT);
        if (cn0)         (*env)->ReleaseDoubleArrayElements(env, jcn0, cn0, JNI_ABORT);
        if (carrierFreq) (*env)->ReleaseDoubleArrayElements(env, jcarrierFreq, carrierFreq, JNI_ABORT);
        if (prRate)      (*env)->ReleaseDoubleArrayElements(env, jprRate, prRate, JNI_ABORT);
        if (adrMeters)   (*env)->ReleaseDoubleArrayElements(env, jadrMeters, adrMeters, JNI_ABORT);
        if (adrStates)   (*env)->ReleaseIntArrayElements(env, jadrStates, adrStates, JNI_ABORT);
        return no_fix_json(env, "JNI array access failed");
    }

    /* ---- Compute GPS time from Android clock ---- */
    /* timeNanos - fullBiasNanos = GPS nanoseconds since GPS epoch (1980-01-06) */
    int64_t gps_ns    = (int64_t)timeNanos - (int64_t)fullBiasNanos;
    double  gps_s     = (double)gps_ns * 1e-9;
    int     gps_week  = (int)(gps_s / 604800.0);
    double  gps_tow   = gps_s - (double)gps_week * 604800.0;
    gtime_t rover_time = gpst2time(gps_week, gps_tow);

    /* ---- Pack rover observations ---- */
    obsd_t rover_obs[MAXOBS];
    int n_rover = 0;
    int i;

    for (i = 0; i < nMeas && n_rover < MAXOBS; i++) {
        int sys = android_const_to_sys(constTypes[i]);
        if (sys == 0) continue;

        /* State filter: require CODE_LOCK (0x01) + TOW_DECODED (0x08) */
        if (!(states[i] & 0x01)) continue;
        if (!(states[i] & 0x08)) continue;

        /* GLONASS: require GLO_TOD_DECODED (0x40) + staged-off check */
        if (sys == SYS_GLO) {
            if (!(states[i] & 0x40)) continue;
            if (ctx->eph_glo == 0) continue;
            /* Also check navsys includes GLONASS */
            if (!(ctx->opt.navsys & SYS_GLO)) continue;
        }

        /* Satellite number (satno returns 0 on fail) */
        int sat = satno(sys, (int)svids[i]);
        if (sat == 0) continue;

        /* Map carrier frequency → observation code + frequency index */
        uint8_t code;
        int freq_idx;
        if (!map_freq_to_code(sys, carrierFreq[i], &code, &freq_idx)) continue;

        /* Wavelength via sat2freq (rtkcmn.c:766) — handles GLONASS FCN internally */
        double freq = sat2freq(sat, code, nav);
        if (freq <= 0.0) continue;
        double wavelength = CLIGHT / freq;

        /* Pseudorange: travel time × speed of light */
        double tRx_s     = gps_s;
        double tSv_s     = (double)recvSvTime[i] * 1e-9;
        double bias_corr = biasNanos * 1e-9;
        double off_corr  = timeOffset[i] * 1e-9;

        double tt = tRx_s - tSv_s - bias_corr - off_corr;

        /* Normalize travel time via fmod (no while loops) */
        if (sys == SYS_GLO) {
            tt = fmod(tt + 43200.0, 86400.0) - 43200.0;
        } else {
            tt = fmod(tt + 302400.0, 604800.0) - 302400.0;
        }

        double pseudorange = tt * CLIGHT;
        if (pseudorange < 1e6 || pseudorange > 1e8) continue;

        /* ---- Fill obsd_t ---- */
        obsd_t *obs = &rover_obs[n_rover];
        memset(obs, 0, sizeof(obsd_t));
        obs->time       = rover_time;
        obs->sat        = (uint8_t)sat;
        obs->rcv        = 1; /* rover */
        obs->P[freq_idx]    = pseudorange;
        obs->code[freq_idx] = code;
        obs->SNR[freq_idx]  = (uint16_t)(cn0[i] * 1000.0);

        /* Carrier phase from ADR */
        if ((adrStates[i] & 0x01) && wavelength > 0.0) { /* ADR_STATE_VALID */
            obs->L[freq_idx] = adrMeters[i] / wavelength;
            /* LLI: cycle slip / half-cycle flags */
            if (adrStates[i] & 0x02) obs->LLI[freq_idx] |= 1; /* ADR_STATE_RESET */
            if (adrStates[i] & 0x04) obs->LLI[freq_idx] |= 1; /* ADR_STATE_CYCLE_SLIP */
            if (!(adrStates[i] & 0x08)) obs->LLI[freq_idx] |= 2; /* !HALF_CYCLE_RESOLVED */
        }

        /* Doppler: D = -pseudorangeRate / wavelength */
        if (wavelength > 0.0) {
            obs->D[freq_idx] = (float)(-prRate[i] / wavelength);
        }

        n_rover++;
    }

    /* ---- Release JNI arrays ---- */
    (*env)->ReleaseIntArrayElements(env, jsvids, svids, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, jconstTypes, constTypes, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, jstates, states, JNI_ABORT);
    (*env)->ReleaseLongArrayElements(env, jrecvSvTime, recvSvTime, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jtimeOffset, timeOffset, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jcn0, cn0, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jcarrierFreq, carrierFreq, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jprRate, prRate, JNI_ABORT);
    (*env)->ReleaseDoubleArrayElements(env, jadrMeters, adrMeters, JNI_ABORT);
    (*env)->ReleaseIntArrayElements(env, jadrStates, adrStates, JNI_ABORT);

    if (n_rover < 4) {
        return no_fix_json(env, "insufficient rover satellites");
    }

    /* ---- Merge rover + base observations ---- */
    obsd_t merged[MAXOBS * 2];
    int n_total = 0;

    memcpy(merged, rover_obs, n_rover * sizeof(obsd_t));
    n_total = n_rover;

    if (ctx->base_nobs > 0 && n_rover > 0) {
        double dt = fabs(timediff(rover_obs[0].time, ctx->base_time));
        if (dt < 2.0) {
            int remaining = MAXOBS * 2 - n_total;
            int base_copy = ctx->base_nobs;
            if (base_copy > remaining) base_copy = remaining;
            if (base_copy > 0) {
                memcpy(merged + n_total, ctx->base_obs,
                       base_copy * sizeof(obsd_t));
                n_total += base_copy;
            }
        }
    }

    /* Set base station position */
    if (ctx->has_base_pos) {
        ctx->rtk.rb[0] = ctx->base_pos[0];
        ctx->rtk.rb[1] = ctx->base_pos[1];
        ctx->rtk.rb[2] = ctx->base_pos[2];
    }

    /* ---- Solve ---- */
    /* rtkpos returns int: 0=no solution, 1=solution available */
    rtkpos(&ctx->rtk, merged, n_total, nav);
    ctx->last_sol_stat = ctx->rtk.sol.stat;

    /* Convert ECEF → lat/lon/alt (ecef2pos is void) */
    double pos[3]; /* {lat_rad, lon_rad, alt_m} */
    ecef2pos(ctx->rtk.sol.rr, pos);
    double lat = pos[0] * R2D;
    double lon = pos[1] * R2D;
    double alt = pos[2];

    /* hasFix: ONLY differential solutions (SOLQ_FIX or SOLQ_FLOAT) */
    int has_fix = (ctx->rtk.sol.stat == SOLQ_FIX ||
                   ctx->rtk.sol.stat == SOLQ_FLOAT);

    int n_used = ctx->rtk.sol.ns;
    double ratio = ctx->rtk.sol.ratio;

    /* Accuracy: RMS of position covariance (approximate) */
    double qr[3];
    qr[0] = ctx->rtk.sol.qr[0]; /* var(x) */
    qr[1] = ctx->rtk.sol.qr[1]; /* var(y) */
    qr[2] = ctx->rtk.sol.qr[2]; /* var(z) */
    double accuracy = sqrt(qr[0] + qr[1] + qr[2]);

    /* Status description */
    const char *stat_str;
    switch (ctx->rtk.sol.stat) {
        case SOLQ_FIX:    stat_str = "fix";    break;
        case SOLQ_FLOAT:  stat_str = "float";  break;
        case SOLQ_SINGLE: stat_str = "single"; break;
        case SOLQ_DGPS:   stat_str = "dgps";   break;
        case SOLQ_SBAS:   stat_str = "sbas";   break;
        case SOLQ_PPP:    stat_str = "ppp";    break;
        default:          stat_str = "none";    break;
    }

    /* Build JSON response */
    char buf[512];
    if (has_fix && accuracy > 0.0) {
        snprintf(buf, sizeof(buf),
            "{\"hasFix\":true,\"lat\":%.9f,\"lon\":%.9f,\"alt\":%.3f,"
            "\"accuracy\":%.3f,\"nSat\":%d,\"ratio\":%.1f,"
            "\"message\":\"%s\"}",
            lat, lon, alt, accuracy, n_used, ratio, stat_str);
    } else {
        snprintf(buf, sizeof(buf),
            "{\"hasFix\":false,\"lat\":%.9f,\"lon\":%.9f,\"alt\":%.3f,"
            "\"accuracy\":null,\"nSat\":%d,\"ratio\":%.1f,"
            "\"message\":\"%s (solstat=%d, rover=%d, base=%d)\"}",
            lat, lon, alt, n_used, ratio, stat_str,
            ctx->rtk.sol.stat, n_rover, ctx->base_nobs);
    }

    LOGD("processEpoch: stat=%s rover=%d base=%d total=%d",
         stat_str, n_rover, ctx->base_nobs, n_total);

    return (*env)->NewStringUTF(env, buf);
}

/* ---- nativeGetStatus ---------------------------------------------------- */

JNIEXPORT jstring JNICALL
Java_day_azimuth_observer_service_ntrip_RtklibNative_nativeGetStatus(
    JNIEnv *env, jobject thiz, jlong handle)
{
    if (handle == 0) {
        return (*env)->NewStringUTF(env,
            "{\"baseObs\":0,\"basePosValid\":false,"
            "\"ephGps\":0,\"ephGlo\":0,\"ephGal\":0,\"ephBds\":0,"
            "\"lastSolStat\":0}");
    }
    azimuth_ctx_t *ctx = (azimuth_ctx_t *)(intptr_t)handle;

    char buf[512];
    snprintf(buf, sizeof(buf),
        "{\"baseObs\":%d,\"basePosValid\":%s,"
        "\"ephGps\":%d,\"ephGlo\":%d,\"ephGal\":%d,\"ephBds\":%d,"
        "\"lastSolStat\":%d}",
        ctx->base_nobs,
        ctx->has_base_pos ? "true" : "false",
        ctx->eph_gps, ctx->eph_glo, ctx->eph_gal, ctx->eph_bds,
        ctx->last_sol_stat);

    return (*env)->NewStringUTF(env, buf);
}

/* ---- deprecated stubs (Phase 3b compat) --------------------------------- */

static const char *NO_FIX_POINT =
    "{\"hasFix\":false,\"lat\":0.0,\"lon\":0.0,\"alt\":0.0,"
    "\"accuracy\":null,\"nSat\":0,\"ratio\":0.0,"
    "\"message\":\"deprecated - use nativeProcessEpoch\"}";

static const char *NO_FIX_RTK =
    "{\"hasFix\":false,\"lat\":0.0,\"lon\":0.0,\"alt\":0.0,"
    "\"accuracy\":null,\"nSat\":0,\"ratio\":0.0,"
    "\"message\":\"deprecated - use nativeProcessEpoch\"}";

JNIEXPORT jstring JNICALL
Java_day_azimuth_observer_service_ntrip_RtklibNative_nativeProcessPoint(
    JNIEnv *env, jobject thiz, jlong handle, jstring obsJson)
{
    return (*env)->NewStringUTF(env, NO_FIX_POINT);
}

JNIEXPORT jstring JNICALL
Java_day_azimuth_observer_service_ntrip_RtklibNative_nativeProcessRtk(
    JNIEnv *env, jobject thiz, jlong handle, jstring roverJson, jstring baseJson)
{
    return (*env)->NewStringUTF(env, NO_FIX_RTK);
}
