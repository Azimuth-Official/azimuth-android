cmake_minimum_required(VERSION 3.18)
project(rtklib C)

add_library(rtklib STATIC
    rtkcmn.c rtkpos.c pntpos.c lambda.c ephemeris.c
    rtcm.c rtcm3.c ionex.c sbas.c preceph.c
    trace.c solution.c ppp.c ppp_ar.c tides.c
)

target_compile_definitions(rtklib PRIVATE
    ENAGLO ENAGAL ENACMP
    NFREQ=3 NEXOBS=0
)
