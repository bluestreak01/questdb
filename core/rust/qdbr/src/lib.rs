
/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

pub extern crate jni;

use jni::sys::jlong;
use jni::{objects::JClass, JNIEnv, JavaVM};

// Static size eq assertion: Ensures we can write pointers in place of jlong in our signatures.
const _: fn() = || {
    let _ = core::mem::transmute::<jlong, *const i32>;
};

static mut JAVA_VM: Option<JavaVM> = None;

pub fn get_jenv() -> Option<JNIEnv<'static>> {
    unsafe {
        JAVA_VM.as_ref().map(|vm| {
            let j_env = vm
                .attach_current_thread_permanently()
                .expect("could not attach jni env");
            j_env
        })
    }
}

/// A macro to be used in JNI functions to unwrap a `jni::errors::Result<T>`.
/// It checks if the result is `Ok(T)` and returns the value, otherwise it
/// will return from the calling function if there's a pending Java exception,
/// or wrap the error in the specified exception class and throw it and return
/// the calling function.
#[macro_export]
macro_rules! unwrap_or_throw {
    ($env:expr, $result:expr, $fallback_java_exception_class:literal $(, $return_sentinel:literal)?) => {
        match $result {
            Ok(result) => result,
            Err(e) => {
                let throwable = $env.exception_occurred().unwrap();
                if throwable.is_null() {
                    $env.throw_new($fallback_java_exception_class, &format!("{}", e))
                        .expect("could not throw java exception");
                }
                return $($return_sentinel)?;
            }
        }
    };
    ($env:expr, $result:expr) => {
        unwrap_or_throw!($env, $result, "java/lang/RuntimeException")
    };
}

#[no_mangle]
pub extern "system" fn Java_io_questdb_std_Os_initRust(mut env: JNIEnv, _class: JClass) {
    if std::env::var("RUST_BACKTRACE").is_err() {
        std::env::set_var("RUST_BACKTRACE", "1");
    }

    let vm = unwrap_or_throw!(env, env.get_java_vm());
    unsafe {
        JAVA_VM = Some(vm);
    }
}

#[no_mangle]
pub extern "system" fn Java_io_questdb_std_Os_rustSmokeTest(
    _env: JNIEnv,
    _class: JClass,
    a: i64,
    b: i64,
) -> i64 {
    a + b
}

#[no_mangle]
pub extern "system" fn Java_io_questdb_std_Os_isRustReleaseBuild(
    _env: JNIEnv,
    _class: JClass,
) -> bool {
    !cfg!(debug_assertions)
}
