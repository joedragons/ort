/*
 * Copyright (C) 2021 Sonatype, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.clients.ossindex

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

import okhttp3.Credentials
import okhttp3.OkHttpClient

import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Interface for the OSS Index REST API, based on the documentation from https://ossindex.sonatype.org/rest.
 */
interface OssIndexService {
    companion object {
        /**
         * The default base URL for the REST API of the public OSS Index service.
         */
        const val DEFAULT_BASE_URL = "https://ossindex.sonatype.org/"

        /**
         * The mapper for JSON (de-)serialization used by this service.
         */
        val JSON_MAPPER = JsonMapper().registerKotlinModule()

        /**
         * Create an OSS Index service instance for communicating with a server running at the given [url], optionally
         * using [user] and [password] for basic authentication, and / or a pre-built OkHttp [client].
         */
        fun create(
            url: String,
            user: String? = null,
            password: String? = null,
            client: OkHttpClient? = null
        ): OssIndexService {
            val ossIndexClient = (client ?: OkHttpClient()).newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    val requestBuilder = request.newBuilder()

                    if (user != null && password != null) {
                        requestBuilder.header("Authorization", Credentials.basic(user, password))
                    }

                    chain.proceed(requestBuilder.build())
                }
                .build()

            val retrofit = Retrofit.Builder()
                .client(ossIndexClient)
                .baseUrl(url)
                .addConverterFactory(JacksonConverterFactory.create(JSON_MAPPER))
                .build()

            return retrofit.create(OssIndexService::class.java)
        }
    }

    // See https://ossindex.sonatype.org/rest#model-ComponentReportRequest.
    data class ComponentReportRequest(
        val coordinates: List<String>
    )

    // See https://ossindex.sonatype.org/rest#model-ComponentReport.
    data class ComponentReport(
        /** The Package URL coordinates. */
        val coordinates: String,

        /** The description of the component. */
        val description: String,

        /** The reference URL of the component on OSS Index itself. */
        val reference: String,

        /** The list of known vulnerabilities. */
        val vulnerabilities: List<Vulnerability>
    )

    // See https://ossindex.sonatype.org/rest#model-ComponentReportVulnerability.
    data class Vulnerability(
        /** A UUID */
        val id: String,

        /** A human-readable name; in case of a CVE the VCE name. */
        val displayName: String,

        /** A title for the vulnerability. */
        val title: String,

        /** A longer description of the vulnerability. */
        val description: String,

        /** A numeric CVSS score. */
        val cvssScore: Float,

        /** A CVSS vector string. */
        val cvssVector: String,

        /** A Common Vulnerabilities and Exposures value, if known. */
        val cve: String?,

        /** A Common Weakness Enumeration value, if known. */
        val cwe: String?,

        /** The reference URL of the vulnerability on OSS Index itself. */
        val reference: String,

        /** An optional list of additional external references. */
        val externalReferences: List<String>?,

        /** An optional list of affected version ranges. */
        val versionRanges: List<String>?
    )

    /**
     * Request vulnerability reports for [components].
     */
    @POST("api/v3/component-report")
    suspend fun getComponentReport(@Body components: ComponentReportRequest): List<ComponentReport>
}