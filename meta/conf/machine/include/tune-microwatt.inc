DEFAULTTUNE ?= "microwatt"

require conf/machine/include/powerpc/arch-powerpc64.inc

TUNEVALID[microwatt] = "Enable Microwatt specific processor optimizations"
TUNE_CCARGS .= "${@bb.utils.contains('TUNE_FEATURES', 'microwatt', ' -mno-altivec -mno-vsx', '', d)}"

AVAILTUNES += "microwatt"

TUNE_FEATURES:tune-microwatt = "m64 fpu-hard microwatt"
BASE_LIB:tune-microwatt = "lib64"
TUNE_PKGARCH:tune-microwatt = "microwatt"
#PACKAGE_EXTRA_ARCHS:tune-microwatt = "${PACKAGE_EXTRA_ARCHS:tune-powerpc64le} microwatt"
PACKAGE_EXTRA_ARCHS:tune-microwatt = "${PACKAGE_EXTRA_ARCHS:tune-powerpc64le}"

# Eventually we'll have a configure option to disable float128, use it here
#GLIBC_EXTRA_OECONF += "${@bb.utils.contains('TUNE_FEATURES', 'microwatt', '--disable-float128-or-whatever-its-called', '', d)}"
