package com.verdant.app.di

import android.content.Context
import androidx.room.Room
import com.verdant.app.data.image.ImageStorage
import com.verdant.app.data.knowledge.AssetKnowledgeBase
import com.verdant.app.data.knowledge.KnowledgeBase
import com.verdant.app.data.local.ScanDao
import com.verdant.app.data.local.VerdantDatabase
import com.verdant.app.domain.Clock
import com.verdant.app.domain.ImageImporter
import com.verdant.app.domain.ScanSaver
import com.verdant.app.data.repository.ScanRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    @Singleton
    abstract fun bindKnowledgeBase(impl: AssetKnowledgeBase): KnowledgeBase
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VerdantDatabase =
        Room.databaseBuilder(context, VerdantDatabase::class.java, VerdantDatabase.NAME)
            // Add proper Migrations here when the schema changes after release.
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()

    @Provides
    fun provideScanDao(db: VerdantDatabase): ScanDao = db.scanDao()

    @Provides
    fun provideImageImporter(storage: ImageStorage): ImageImporter =
        ImageImporter { uri -> storage.import(android.net.Uri.parse(uri)) }

    @Provides
    fun provideScanSaver(repository: ScanRepository): ScanSaver = ScanSaver(repository::save)

    @Provides
    fun provideClock(): Clock = Clock { System.currentTimeMillis() }
}
