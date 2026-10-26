package gov.anzong.androidnga.activity.compose.drawer

import com.justwent.androidnga.bu.UserManager
import gov.anzong.androidnga.http.OnHttpCallBack
import sp.phone.http.bean.ProfileData
import sp.phone.task.JsonProfileLoadTask
import sp.phone.util.FunctionUtils

/**
 * Fills in the avatar of logged-in accounts that have none stored.
 *
 * Login stores only uid, cid and nickname; the avatar URL used to be written only when the
 * user opened their own profile page (ProfileActivity.handleAvatar), so a freshly logged-in
 * account showed the default picture in the drawer indefinitely. This runs the same profile
 * request once per account and process, and stores the result through the same
 * UserManager.setAvatarUrl path, which updates the drawer through its LiveData.
 */
object DrawerAvatarLoader {

    private val requested = HashSet<String>()

    fun loadMissing() {
        val users = UserManager.getUserListLiveData().value.orEmpty()
        users.filter { it.userId.isNotEmpty() && it.avatarUrl.isNullOrEmpty() }
            .forEach { user ->
                if (!requested.add(user.userId)) return@forEach
                JsonProfileLoadTask(object : OnHttpCallBack<ProfileData> {
                    override fun onSuccess(data: ProfileData) {
                        val url = FunctionUtils.parseAvatarUrl(data.avatarUrl)
                        if (!url.isNullOrEmpty()) {
                            UserManager.setAvatarUrl(user.userId, url)
                            // setAvatarUrl mutates the user in place; republish so observers redraw.
                            UserManager.getUserListLiveData().value = UserManager.getUserList().toList()
                        }
                    }

                    override fun onError(text: String) {
                        // Allow a later retry (e.g. after network recovers) in this process.
                        requested.remove(user.userId)
                    }
                }).execute("uid=" + user.userId)
            }
    }
}
