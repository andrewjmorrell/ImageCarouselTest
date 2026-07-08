package com.example.imagecarousel.data.mediastore
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Images.Thumbnails
import android.content.ContentUris
import android.graphics.BitmapFactory
import android.graphics.Bitmap

import android.content.Context
import android.util.Log
import com.example.imagecarousel.data.mappers.toDomain
import com.example.imagecarousel.data.models.ImageDto
import com.example.imagecarousel.data.models.ImageResponseDto
import com.example.imagecarousel.domain.ImageResponse
import com.example.imagecarousel.domain.repository.ImageRepository
import javax.inject.Inject

// Additional imports for permissions and query args
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.content.ContentResolver
import androidx.core.content.ContextCompat

//Stubbed out repository that could be used to load images from the users photos
class MediaStoreImageRepository @Inject constructor(private val context: Context) : ImageRepository {
    override suspend fun getImages(count: Int): ImageResponse {

        Log.e("tag", "calling getimages")
        val images = mutableListOf<ImageDto>()

        // Runtime permission check: required to read device images
        val hasPermission = when {
            Build.VERSION.SDK_INT >= 33 -> ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            else -> ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) {
            Log.w("MediaStoreRepo", "Missing read permission; returning empty list")
            return ImageResponseDto(emptyList()).toDomain()
        }

        val collection: Uri = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        // Columns we care about
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )

        // Sort newest first
        val sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC"

        val resolver = context.contentResolver

        val cursor = if (Build.VERSION.SDK_INT >= 29) {
            val args = Bundle().apply {
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Images.Media.DATE_ADDED))
                putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                putInt(ContentResolver.QUERY_ARG_LIMIT, count)
                // Limit to the Pictures directory on API 29+
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf("Pictures/%"))
            }
            resolver.query(collection, projection, args, null)
        } else {
            // Pre-29 fallback: no RELATIVE_PATH; sort string + manual limit
            resolver.query(
                collection,
                projection,
                null,
                null,
                sortOrder
            )
        }

        cursor?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            var added = 0
            while (c.moveToNext() && (Build.VERSION.SDK_INT >= 29 || added < count)) {
                val id = c.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                val displayName = c.getString(nameCol)
                val mime = c.getString(mimeCol)
                val dateAdded = c.getLong(dateCol)
                val width = runCatching { c.getInt(wCol) }.getOrNull()
                val height = runCatching { c.getInt(hCol) }.getOrNull()
                val size = runCatching { c.getLong(sizeCol) }.getOrNull()

                val bitmap = runCatching {
                    resolver.openInputStream(contentUri)?.use { ins ->
                        BitmapFactory.decodeStream(ins)
                    }
                }.getOrNull()

                Log.e("tag", "adding image bitmap=${bitmap}")

                images += ImageDto(
//                    id = id,
//                    uri = contentUri.toString(),
//                    displayName = displayName,
//                    mimeType = mime,
//                    width = width,
//                    height = height,
//                    sizeBytes = size,
//                    dateAddedSeconds = dateAdded,
                    image = bitmap!!
                )
                added++
            }
        }

        return ImageResponseDto(images).toDomain()
    }
}