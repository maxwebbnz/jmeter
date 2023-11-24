/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "org.apache.jmeter.build-logic"

dependencies {
    // We use precompiled script plugins (== plugins written as src/kotlin/build-logic.*.gradle.kts files,
    // and we need to declare dependency on org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin
    // to make it work.
    // See https://github.com/gradle/gradle/issues/17016 regarding expectedKotlinDslPluginsVersion
    implementation("org.gradle.kotlin.kotlin-dsl:org.gradle.kotlin.kotlin-dsl.gradle.plugin:$expectedKotlinDslPluginsVersion")
    // It seems to be the best way to make KotlinCompile available for use in build-logic.kotlin-dsl-gradle-plugin.gradle.kts
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:$embeddedKotlinVersion")
}

// We need to figure out a version that is supported by the current JVM, and by the Kotlin Gradle plugin
val currentJava = JavaVersion.current()
if (currentJava > JavaVersion.VERSION_1_8) {
    // We want an LTS Java release for build script compilation
    val latestSupportedLts = listOf("17", "11")
        .intersect(JvmTarget.values().mapTo(mutableSetOf()) { it.target })
        .first { JavaVersion.toVersion(it) <= currentJava }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(JavaVersion.toVersion(latestSupportedLts).majorVersion.toInt())
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = latestSupportedLts
        }
    }
}
