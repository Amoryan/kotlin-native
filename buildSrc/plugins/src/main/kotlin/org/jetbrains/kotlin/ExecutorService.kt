/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager

interface ExecutorService {
    fun execute(closure: Closure<in ExecSpec>): ExecResult? = execute(ConfigureUtil.configureUsing(closure))
    fun execute(action: Action<in ExecSpec>): ExecResult?
}

fun getExecutor(project: Project): ExecutorService {
    val platformManager = project.rootProject.findProperty("platformManager") as PlatformManager
    val testTarget = platformManager.targetManager(project.findProperty("testTarget") as String?).target
    val platform = platformManager.platform(testTarget)
    val absoluteTargetToolchain = platform.absoluteTargetToolchain
    val absoluteTargetSysRoot = platform.absoluteTargetSysRoot

    return when (testTarget) {
        KonanTarget.WASM32 -> object : ExecutorService {
            override fun execute(action: Action<in ExecSpec>): ExecResult? = project.exec { execSpec ->
                action.execute(execSpec)
                with(execSpec) {
                    val exe = executable
                    val d8 = "$absoluteTargetToolchain/bin/d8"
                    val launcherJs = "$executable.js"
                    executable = d8
                    args = listOf("--expose-wasm", launcherJs, "--", exe) + args
                }
            }
        }
        KonanTarget.LINUX_MIPS32, KonanTarget.LINUX_MIPSEL32 -> object : ExecutorService {
            override fun execute(action: Action<in ExecSpec>): ExecResult? = project.exec { execSpec ->
                action.execute(execSpec)
                with(execSpec) {
                    val qemu = if (platform.target === KonanTarget.LINUX_MIPS32) "qemu-mips" else "qemu-mipsel"
                    val absoluteQemu = "$absoluteTargetToolchain/bin/$qemu"
                    val exe = executable
                    executable = absoluteQemu
                    args = listOf("-L", absoluteTargetSysRoot, exe) + args
                }
            }
        }
        KonanTarget.IOS_X64 -> ExecSimulator(project)
        else -> ExecRemote(project)
    }
}