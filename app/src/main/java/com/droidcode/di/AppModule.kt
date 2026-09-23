package com.droidcode.di

import android.content.Context
import com.droidcode.editor.EditorManager
import com.droidcode.filesystem.LocalFileSystem
import com.droidcode.project.WorkspaceManager
import com.droidcode.settings.SettingsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWorkspaceManager(): WorkspaceManager = WorkspaceManager.getInstance()

    @Provides
    @Singleton
    fun provideEditorManager(): EditorManager = EditorManager.getInstance()

    @Provides
    @Singleton
    fun provideSettingsManager(@ApplicationContext context: Context): SettingsManager =
        SettingsManager.getInstance(context)

    @Provides
    @Singleton
    fun provideLocalFileSystem(): LocalFileSystem = LocalFileSystem.getInstance()
}
