/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/


#ifndef OLM_EXPORT_H
#define OLM_EXPORT_H

#ifdef OLM_STATIC_DEFINE
#  define OLM_EXPORT
#  define OLM_NO_EXPORT
#else
#  ifndef OLM_EXPORT
#    ifdef olm_EXPORTS
        /* We are building this library */
#      define OLM_EXPORT __attribute__((visibility("default")))
#    else
        /* We are using this library */
#      define OLM_EXPORT __attribute__((visibility("default")))
#    endif
#  endif

#  ifndef OLM_NO_EXPORT
#    define OLM_NO_EXPORT __attribute__((visibility("hidden")))
#  endif
#endif

#ifndef OLM_DEPRECATED
#  define OLM_DEPRECATED __attribute__ ((__deprecated__))
#endif

#ifndef OLM_DEPRECATED_EXPORT
#  define OLM_DEPRECATED_EXPORT OLM_EXPORT OLM_DEPRECATED
#endif

#ifndef OLM_DEPRECATED_NO_EXPORT
#  define OLM_DEPRECATED_NO_EXPORT OLM_NO_EXPORT OLM_DEPRECATED
#endif

#if 0 /* DEFINE_NO_DEPRECATED */
#  ifndef OLM_NO_DEPRECATED
#    define OLM_NO_DEPRECATED
#  endif
#endif

#endif /* OLM_EXPORT_H */
