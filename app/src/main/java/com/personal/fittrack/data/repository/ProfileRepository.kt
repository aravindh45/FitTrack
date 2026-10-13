package com.personal.fittrack.data.repository

import com.personal.fittrack.data.local.UserProfileDao
import com.personal.fittrack.data.local.toDomain
import com.personal.fittrack.data.local.toEntity
import com.personal.fittrack.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepository(private val dao: UserProfileDao) {

    fun observeProfile(): Flow<UserProfile?> = dao.observeProfile().map { it?.toDomain() }

    suspend fun getProfileOnce(): UserProfile? = dao.getProfileOnce()?.toDomain()

    suspend fun saveProfile(profile: UserProfile) = dao.upsert(profile.toEntity())
}
