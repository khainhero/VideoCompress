package com.example.video_compress

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.otaliastudios.transcoder.Transcoder
import com.otaliastudios.transcoder.TranscoderListener
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategies
import com.otaliastudios.transcoder.strategy.DefaultVideoStrategy
import com.otaliastudios.transcoder.strategy.TrackStrategy
import com.otaliastudios.transcoder.strategy.size.*
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


/**
 * VideoCompressPlugin
 */
class VideoCompressPlugin private constructor(private val activity: Activity, private val context: Context, private val channel: MethodChannel) : MethodCallHandler {

    var channelName = "video_compress"
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

        when (call.method) {
            "getThumbnail" -> {
                val path = call.argument<String>("path")
                val quality = call.argument<Int>("quality")!!
                val position = call.argument<Int>("position")!! // to long
                ThumbnailUtility(channelName).getThumbnail(path!!, quality, position.toLong(), result)
            }
            "getThumbnailWithFile" -> {
                val path = call.argument<String>("path")
                val quality = call.argument<Int>("quality")!!
                val position = call.argument<Int>("position")!! // to long
                ThumbnailUtility("video_compress").getThumbnailWithFile(context, path!!, quality,
                        position.toLong(), result)
            }
            "getMediaInfo" -> {
                val path = call.argument<String>("path")
                result.success(Utility(channelName).getMediaInfoJson(context, path!!).toString())
            }
            "deleteAllCache" -> {
                result.success(Utility(channelName).deleteAllCache(context, result));
            }
            "compressVideo" -> {
                val path = call.argument<String>("path")!!
                val quality = call.argument<Int>("quality")!!
                val deleteOrigin = call.argument<Boolean>("deleteOrigin")!!
                val startTime = call.argument<Int>("startTime")
                val duration = call.argument<Int>("duration")
                val includeAudio = call.argument<Boolean>("includeAudio")
                val frameRate = if (call.argument<Int>("frameRate")==null) 30 else call.argument<Int>("frameRate")
                val tempDir: String = this.context.getExternalFilesDir("video_compress")!!.absolutePath
                val out = SimpleDateFormat("yyyy-MM-dd hh-mm-ss").format(Date())
                val destPath: String = tempDir + File.separator + "VID_" + out + ".mp4"

                var strategy: TrackStrategy = DefaultVideoStrategy.atMost(540).build();

                when (quality) {

                   0 -> {
                      strategy = DefaultVideoStrategy.atMost(540).build()
                   }

                    1 -> {
                        val bitRateCalculation = 0.07F * (600*504*2F*frameRate!!)
                        strategy = DefaultVideoStrategy.atMost(504)
                            .frameRate(frameRate!!)
                            .bitRate(bitRateCalculation.toLong())
                            .build()
                        //strategy = DefaultVideoStrategy.atMost(480).build()
                    }
                    2 -> {
                        val bitRateCalculation = 0.07F * (650*540*2F*frameRate!!)
                        /*strategy = DefaultVideoStrategy.exact(360, 640)
                            .bitRate(bitRateCalculation.toLong())
                            .frameRate(frameRate!!)
                            .keyFrameInterval(3F)
                            .build() */
                        strategy = DefaultVideoStrategy.atMost(540)
                            .frameRate(frameRate!!)
                            .bitRate(bitRateCalculation.toLong())
                            .build()
                    }
                    3 -> {
                        val bitRateCalculation = 0.07F * (720 * 1280 * 2F*frameRate!!)
                        strategy = DefaultVideoStrategy.atMost(720)
                            .frameRate(frameRate!!)
                            .bitRate(bitRateCalculation.toLong())
                            .build()
                        /*assert(value = frameRate != null)
                        strategy = DefaultVideoStrategy.Builder()
                                .keyFrameInterval(3f)
                                .bitRate(1000 * 1000 * 2.toLong())
                                .frameRate(frameRate!!) // will be capped to the input frameRate
                                .build()*/
                    }
                }


                Transcoder.into(destPath!!)
                        .addDataSource(context, Uri.parse(path))
                        .setVideoTrackStrategy(strategy)
                        .setListener(object : TranscoderListener {
                            override fun onTranscodeProgress(progress: Double) {
                                channel.invokeMethod("updateProgress", progress * 100.00)
                            }
                            override fun onTranscodeCompleted(successCode: Int) {
                                channel.invokeMethod("updateProgress", 100.00)
                                val json = Utility(channelName).getMediaInfoJson(context, destPath)
                                json.put("isCancel", false)
                                result.success(json.toString())
                                if (deleteOrigin) {
                                    File(path).delete()
                                }
                            }

                            override fun onTranscodeCanceled() {
                                result.success(null)
                            }

                            override fun onTranscodeFailed(exception: Throwable) {
                                result.success(null)
                            }
                        }).transcode()
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    companion object {
        const val ACTIVITY_2_REQUEST = 999

        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "video_compress")
            val instance = VideoCompressPlugin(registrar.activity(), registrar.context(), channel)
            channel.setMethodCallHandler(instance)
        }
    }

}
