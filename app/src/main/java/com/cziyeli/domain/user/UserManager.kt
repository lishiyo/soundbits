package com.cziyeli.domain.user

import android.content.Context
import com.cziyeli.commons.AUTH_TOKEN
import com.cziyeli.commons.LOGIN_EXPIRATION
import com.cziyeli.commons.bindSharedPreference
import com.cziyeli.commons.di.ForApplication
import com.cziyeli.domain.SimpleImage
import kaaes.spotify.webapi.android.models.UserPrivate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Equivalent of UserPublic/UserPrivate
 */
@Singleton
class UserManager @Inject constructor(@ForApplication context: Context) {
    private val TAG = UserManager::class.simpleName

    var CURRENT_USER: User? = null

    // Auth requirements
    var nextExpirationSeconds: Long by bindSharedPreference(context, LOGIN_EXPIRATION, 0)
    var accessToken: String by bindSharedPreference(context, AUTH_TOKEN, "")

    // Might not have gotten /me yet, but has access tokens
    fun isAccessTokenValid() : Boolean {
        return !accessToken.isEmpty() && (System.currentTimeMillis() / 1000) < nextExpirationSeconds
    }

    fun isLoggedIn() : Boolean {
        return CURRENT_USER != null && isAccessTokenValid()
    }

    fun getCurrentUser() : User {
        return CURRENT_USER!!
    }
}

data class User(
        val display_name: String,
        val id: String,
        val email: String,
        val external_urls: Map<String, String>? = null,
        val country: String,
        val product: String,
        val href: String,
        val uri: String,
        val images: List<SimpleImage> = listOf()
) {
    val cover_image: String?
        get() = if (images.isEmpty()) "" else images[0].url

    val external_url: String?
        get() = external_urls?.getOrDefault("spotify", "")

    companion object {
        fun create(apiModel: UserPrivate) : User {
            return User(apiModel.display_name, apiModel.id, apiModel.email, apiModel.external_urls, apiModel.country,
                    apiModel.product, apiModel.href,
                    apiModel.uri, apiModel.images.map { SimpleImage(it) })
        }

        fun create(display_name: String,
                   id: String,
                   email: String,
                   external_urls: Map<String, String>? = null,
                   country: String,
                   product: String,
                   href: String,
                   uri: String,
                   images: List<SimpleImage> = listOf()) : User {
            return User(display_name, id, email, external_urls, country, product, href, uri, images)
        }
    }
}