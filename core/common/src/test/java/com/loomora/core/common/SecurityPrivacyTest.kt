package com.loomora.core.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SecurityPrivacyTest {

    @Test
    fun pathTraversalGuard_rejectsParentDirectoryTraversal() {
        val rootDir = File("/data/user/0/com.loomora/app_recordings")
        val maliciousFile = File("/data/user/0/com.loomora/app_recordings/../shared_prefs/secret.xml")

        val isSafe = maliciousFile.canonicalPath.startsWith(rootDir.canonicalPath)
        assertFalse("Path traversal attempt must be rejected", isSafe)
    }

    @Test
    fun pathTraversalGuard_allowsLegitimateSubfile() {
        val rootDir = File("/data/user/0/com.loomora/app_recordings")
        val validFile = File("/data/user/0/com.loomora/app_recordings/rec_12345.m4a")

        val isSafe = validFile.canonicalPath.startsWith(rootDir.canonicalPath)
        assertTrue("Legitimate subfile path must be allowed", isSafe)
    }
}
