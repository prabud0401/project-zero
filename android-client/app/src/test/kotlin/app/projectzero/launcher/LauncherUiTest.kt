package app.projectzero.launcher

import app.projectzero.domain.notification.Sensitivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockScreenRedactorTest {
    @Test
    fun publicVisibleWhileLocked() {
        val view = LockScreenRedactor.forDisplay("sys", "ok", Sensitivity.PUBLIC, true)
        assertFalse(view.redacted)
        assertTrue(view.headline == "sys")
    }

    @Test
    fun personalHiddenWhileLocked() {
        val view = LockScreenRedactor.forDisplay("secret title", "secret body", Sensitivity.PERSONAL, true)
        assertTrue(view.redacted)
        assertTrue(view.headline == LockScreenRedactor.LOCKED_HEADLINE)
        assertFalse(view.summary.contains("secret"))
    }
}
