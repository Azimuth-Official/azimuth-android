/*
 * Azimuth RTK solver context — Phase 3c data plumbing.
 *
 * Holds persistent state across JNI calls: dual RTCM decoders (base obs +
 * ephemeris), RTK Kalman filter, latest base observations, station position,
 * and ephemeris counters.
 *
 * Memory ownership:
 *   - Each rtcm_t owns its internal nav_t arrays (allocated by init_rtcm,
 *     freed by free_rtcm).  Never shallow-copy nav_t.
 *   - rtk_t owns Kalman filter state (allocated by rtkinit, freed by rtkfree).
 *   - rtkpos() takes const nav_t* — read-only, no ownership transfer.
 *   - The authoritative nav source for rtkpos() is &ctx->rtcm_eph.nav.
 *
 * License: internal to the Azimuth project.
 */

#ifndef AZIMUTH_CTX_H
#define AZIMUTH_CTX_H

#include "rtklib-core/rtklib.h"

/* Stream kind — passed from Kotlin RtklibNative.STREAM_* constants */
#define STREAM_BASE_OBS  1   /* observation mount: nearby base station */
#define STREAM_EPHEMERIS 2   /* ephemeris mount: any mount with 1019/1020/etc */
#define STREAM_COMBINED  3   /* single mount providing both obs + eph */

/* Return bitmask from nativeFeedRtcm */
#define FEED_OBS_COMPLETE  0x01  /* base observation epoch decoded */
#define FEED_EPH_UPDATED   0x02  /* ephemeris message decoded */
#define FEED_STATION_INFO  0x04  /* station position (1005/1006) decoded */

typedef struct {
    rtk_t    rtk;              /* RTK solver state (Kalman filter, solution) */
    prcopt_t opt;              /* processing options */
    rtcm_t   rtcm_base;       /* decoder for base observation stream */
    rtcm_t   rtcm_eph;        /* decoder for ephemeris / combined stream */
    obsd_t   base_obs[MAXOBS]; /* latest base observation epoch (rcv=2) */
    int      base_nobs;        /* count of obs in latest base epoch */
    gtime_t  base_time;        /* timestamp of latest base epoch */
    double   base_pos[3];      /* base station ECEF from RTCM 1006 (m) */
    int      has_base_pos;     /* 1 if base_pos is valid */
    int      eph_gps;          /* GPS ephemeris update count */
    int      eph_glo;          /* GLONASS ephemeris update count */
    int      eph_gal;          /* Galileo ephemeris update count */
    int      eph_bds;          /* BeiDou ephemeris update count */
    int      last_sol_stat;    /* last solution status (SOLQ_*) */
} azimuth_ctx_t;

#endif /* AZIMUTH_CTX_H */
