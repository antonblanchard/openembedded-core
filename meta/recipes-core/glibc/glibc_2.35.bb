require glibc.inc
require glibc-version.inc

CVE_CHECK_IGNORE += "CVE-2020-10029 CVE-2021-27645"

# glibc https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2019-1010022
# glibc https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2019-1010023
# glibc https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2019-1010024
# Upstream glibc maintainers dispute there is any issue and have no plans to address it further.
# "this is being treated as a non-security bug and no real threat."
CVE_CHECK_IGNORE += "CVE-2019-1010022 CVE-2019-1010023 CVE-2019-1010024"

# glibc https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2019-1010025
# Allows for ASLR bypass so can bypass some hardening, not an exploit in itself, may allow
# easier access for another. "ASLR bypass itself is not a vulnerability."
# Potential patch at https://sourceware.org/bugzilla/show_bug.cgi?id=22853
CVE_CHECK_IGNORE += "CVE-2019-1010025"

DEPENDS += "gperf-native bison-native"

NATIVESDKFIXES ?= ""
NATIVESDKFIXES:class-nativesdk = "\
           file://0003-nativesdk-glibc-Look-for-host-system-ld.so.cache-as-.patch \
           file://0004-nativesdk-glibc-Fix-buffer-overrun-with-a-relocated-.patch \
           file://0005-nativesdk-glibc-Raise-the-size-of-arrays-containing-.patch \
           file://0006-nativesdk-glibc-Allow-64-bit-atomics-for-x86.patch \
           file://0007-nativesdk-glibc-Make-relocatable-install-for-locales.patch \
           file://0008-nativesdk-glibc-Fall-back-to-faccessat-on-faccess2-r.patch \
"

SRC_URI =  "${GLIBC_GIT_URI};branch=${SRCBRANCH};name=glibc \
           file://etc/ld.so.conf \
           file://generate-supported.mk \
           file://makedbs.sh \
           \
           ${NATIVESDKFIXES} \
           file://0009-yes-within-the-path-sets-wrong-config-variables.patch \
           file://0010-eglibc-Cross-building-and-testing-instructions.patch \
           file://0011-eglibc-Help-bootstrap-cross-toolchain.patch \
           file://0012-eglibc-Resolve-__fpscr_values-on-SH4.patch \
           file://0013-eglibc-Forward-port-cross-locale-generation-support.patch \
           file://0014-localedef-add-to-archive-uses-a-hard-coded-locale-pa.patch \
           file://0016-locale-prevent-maybe-uninitialized-errors-with-Os-BZ.patch \
           file://0017-readlib-Add-OECORE_KNOWN_INTERPRETER_NAMES-to-known-.patch \
           file://0018-wordsize.h-Unify-the-header-between-arm-and-aarch64.patch \
           file://0019-powerpc-Do-not-ask-compiler-for-finding-arch.patch \
           file://0021-Replace-echo-with-printf-builtin-in-nscd-init-script.patch \
           file://0022-sysdeps-gnu-configure.ac-Set-libc_cv_rootsbindir-onl.patch \
           file://0023-timezone-Make-shell-interpreter-overridable-in-tzsel.patch \
           file://0024-fix-create-thread-failed-in-unprivileged-process-BZ-.patch \
           file://0004-Hack-out-float128-support.patch \
           "
S = "${WORKDIR}/git"
B = "${WORKDIR}/build-${TARGET_SYS}"

PACKAGES_DYNAMIC = ""

# the -isystem in bitbake.conf screws up glibc do_stage
BUILD_CPPFLAGS = "-I${STAGING_INCDIR_NATIVE}"
TARGET_CPPFLAGS = "-I${STAGING_DIR_TARGET}${includedir}"

GLIBC_BROKEN_LOCALES = ""

GLIBCPIE ??= ""

EXTRA_OECONF = "--enable-kernel=${OLDEST_KERNEL} \
                --disable-profile \
                --disable-debug --without-gd \
                --enable-clocale=gnu \
                --with-headers=${STAGING_INCDIR} \
                --without-selinux \
                --enable-tunables \
                --enable-bind-now \
                --enable-stack-protector=strong \
                --disable-crypt \
                --with-default-link \
                ${@bb.utils.contains_any('SELECTED_OPTIMIZATION', '-O0 -Og', '--disable-werror', '', d)} \
                ${GLIBCPIE} \
                ${GLIBC_EXTRA_OECONF}"

EXTRA_OECONF += "${@get_libc_fpu_setting(bb, d)}"

EXTRA_OECONF:append:x86 = " ${@bb.utils.contains_any('TUNE_FEATURES', 'i586 c3', '--disable-cet', '--enable-cet', d)}"
EXTRA_OECONF:append:x86-64 = " --enable-cet"

PACKAGECONFIG ??= "nscd memory-tagging"
PACKAGECONFIG[nscd] = "--enable-nscd,--disable-nscd"
PACKAGECONFIG[memory-tagging] = "--enable-memory-tagging,--disable-memory-tagging"

do_patch:append() {
    bb.build.exec_func('do_fix_readlib_c', d)
}

do_fix_readlib_c () {
	sed -i -e 's#OECORE_KNOWN_INTERPRETER_NAMES#${EGLIBC_KNOWN_INTERPRETER_NAMES}#' ${S}/elf/readlib.c
}

do_configure () {
# override this function to avoid the autoconf/automake/aclocal/autoheader
# calls for now
# don't pass CPPFLAGS into configure, since it upsets the kernel-headers
# version check and doesn't really help with anything
        (cd ${S} && gnu-configize) || die "failure in running gnu-configize"
        find ${S} -name "configure" | xargs touch
        CPPFLAGS="" oe_runconf
}

LDFLAGS += "-fuse-ld=bfd"
do_compile () {
	base_do_compile
	echo "Adjust ldd script"
	if [ -n "${RTLDLIST}" ]
	then
		prevrtld=`cat ${B}/elf/ldd | grep "^RTLDLIST=" | sed 's#^RTLDLIST="\?\([^"]*\)"\?$#\1#'`
		# remove duplicate entries
		newrtld=`echo $(printf '%s\n' ${prevrtld} ${RTLDLIST} | LC_ALL=C sort -u)`
		echo "ldd \"${prevrtld} ${RTLDLIST}\" -> \"${newrtld}\""
		sed -i ${B}/elf/ldd -e "s#^RTLDLIST=.*\$#RTLDLIST=\"${newrtld}\"#"
	fi
}

require glibc-package.inc

BBCLASSEXTEND = "nativesdk"
