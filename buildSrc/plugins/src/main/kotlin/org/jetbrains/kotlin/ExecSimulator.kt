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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.PlatformManager

class ExecSimulator(val project: Project) : ExecutorService {

    override fun execute(action: Action<in ExecSpec>): ExecResult? {
        return simctl("list")
    }

    fun xcrun(vararg args: String) = project.exec { it.setCommandLine("/usr/bin/xcrun", *args) }

    fun simctl(vararg args: String) = xcrun("simctl", *args)
}