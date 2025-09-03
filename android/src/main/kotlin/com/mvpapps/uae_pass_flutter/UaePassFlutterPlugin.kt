package com.mvpapps.uae_pass_flutter

import ae.sdg.libraryuaepass.*
import ae.sdg.libraryuaepass.UAEPassController.getAccessToken
import ae.sdg.libraryuaepass.UAEPassController.getAccessCode
import ae.sdg.libraryuaepass.UAEPassController.resume
import ae.sdg.libraryuaepass.UAEPassController.getUserProfile
import ae.sdg.libraryuaepass.UAEPassController.signDocument
import ae.sdg.libraryuaepass.business.profile.model.ProfileModel
import ae.sdg.libraryuaepass.business.authentication.model.UAEPassAccessTokenRequestModel
import ae.sdg.libraryuaepass.business.documentsigning.model.DocumentSigningRequestParams
import ae.sdg.libraryuaepass.business.documentsigning.model.UAEPassDocumentSigningRequestModel
import ae.sdg.libraryuaepass.business.profile.model.UAEPassProfileRequestModel
import ae.sdg.libraryuaepass.business.Environment
import ae.sdg.libraryuaepass.business.Language
import ae.sdg.libraryuaepass.utils.Utils.generateRandomString
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.annotation.NonNull
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import java.io.File
import java.util.*
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/** UaePassPlugin */
class UaePassFlutterPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.NewIntentListener, PluginRegistry.ActivityResultListener {

    private lateinit var channel: MethodChannel
    private lateinit var requestModel: UAEPassAccessTokenRequestModel

    private var client_id: String? = null
    private var client_secret: String? = null
    private var redirect_url: String? = "https://oauthtest.com/authorization/return"
    private var environment: Environment = Environment.STAGING
    private var state: String? = null
    private var scheme: String? = null
    private var failureHost: String? = null
    private var successHost: String? = null
    private var scope: String? = "urn:uae:digitalid:profile"
    private var language: String? = "en"

    private val UAE_PASS_PACKAGE_ID = "ae.uaepass.mainapp"
    private val UAE_PASS_QA_PACKAGE_ID = "ae.uaepass.mainapp.qa"
    private val UAE_PASS_STG_PACKAGE_ID = "ae.uaepass.mainapp.stg"

    private val DOCUMENT_SIGNING_SCOPE = "urn:safelayer:eidas:sign:process:document"
    private val RESPONSE_TYPE = "code"
    private val SCOPE = "urn:uae:digitalid:profile"
    private val ACR_VALUES_MOBILE = "urn:digitalid:authentication:flow:mobileondevice"
    private val ACR_VALUES_WEB = "urn:safelayer:tws:policies:authentication:level:low"

    private var activity: Activity? = null
    private lateinit var result: Result
    private var pendingResult: MethodChannel.Result? = null
    private val TAG = "UAEPassPlugin"

    override fun onAttachedToActivity(@NonNull binding: ActivityPluginBinding) {
        if (activity == null)
            activity = binding.activity
        binding.addOnNewIntentListener(this)
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
    }

    override fun onReattachedToActivityForConfigChanges(@NonNull binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addOnNewIntentListener(this)
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "uae_pass")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        this.result = result
        when (call.method) {
            "set_up_environment" -> {
                CookieManager.getInstance().removeAllCookies { }
                CookieManager.getInstance().flush()
                client_id = call.argument<String>("client_id")
                client_secret = call.argument<String>("client_secret")
                redirect_url = call.argument<String?>("redirect_url")
                environment =
                    if (call.argument<String>("environment") != null && call.argument<String>("environment") == "production") Environment.PRODUCTION else Environment.STAGING
                state = call.argument<String?>("state")
                scheme = call.argument<String>("scheme")
                failureHost = call.argument<String?>("failureHost")
                successHost = call.argument<String?>("successHost")
                scope = call.argument<String?>("scope")
                language = call.argument<String?>("language")

                if (redirect_url == null) {
                    redirect_url = "https://oauthtest.com/authorization/return"
                }
                if (state == null) {
                    state = generateRandomString(24)
                }

                if (failureHost == null) {
                    failureHost = "failure"
                }
                if (successHost == null) {
                    successHost = "success"
                }
                result.success(null)
            }
            "sign_out" -> {
                CookieManager.getInstance().removeAllCookies { }
                CookieManager.getInstance().flush()
                result.success(null)
            }
            "sign_in" -> {
                val authUrl = buildAuthUrl()
                val intent = Intent(activity, UAEPassWebViewActivity::class.java).apply {
                    putExtra(UAEPassWebViewActivity.EXTRA_AUTH_URL, authUrl)
                    putExtra(UAEPassWebViewActivity.EXTRA_REDIRECT_URI, redirect_url)
                    putExtra(UAEPassWebViewActivity.EXTRA_SCHEME, scheme)
                }
                activity?.startActivityForResult(intent, 1001)
                pendingResult = result
            }
            "access_token" -> {
                val code = call.argument<String>("code")
                if (code != null) {
                    requestModel = getAuthenticationRequestModel(activity!!)
                    UAEPassController.getAccessToken(activity!!, requestModel, object : UAEPassAccessTokenCallback {
                        override fun getToken(accessToken: String?, state: String, error: String?) {
                            if (error != null) {
                                result.error("ERROR", error, null);
                            } else {
                                result.success(accessToken)
                            }
                        }
                    })
                } else {
                    result.error("ERROR", "Access code is null", null)
                }
            }
            "profile" -> {
                val accessToken = call.argument<String>("token")
                if (accessToken != null) {
                    val requestModel = getProfileRequestModel(activity!!)
                    UAEPassController.getUserProfile(activity!!, requestModel, object : UAEPassProfileCallback {
                        override fun getProfile(
                            profileModel: ProfileModel?,
                            state: String,
                            error: String?
                        ) {
                            Log.d("TAG", "error $error")
                            if (error != null) {
                                result.error("ERROR", error, null);
                            } else {
                                val gson = Gson()
                                val profileJson = gson.toJson(profileModel)
                                result.success(profileJson)
                            }
                        }
                    })
                } else {
                    result.error("ERROR", "Access token is null", null)
                }
            }
            "sign_document" -> {
                Log.d(TAG, "Method 'sign_document' called from Flutter.")
                val file = loadDocumentFromAssets()
                val finishCallbackUrl = call.argument<String>("finishCallbackUrl")
                Log.d(TAG, "Document loaded from assets: ${file.path}")

                if (finishCallbackUrl != null) {
                    Log.d(TAG, "finishCallbackUrl is not null. Value: $finishCallbackUrl")
                    val documentSigningParams = loadDocumentSigningJson(finishCallbackUrl)
                    documentSigningParams?.let {
                        Log.d(TAG, "Document signing parameters loaded successfully.")
                        val requestModel = UAEPassRequestModels.getDocumentRequestModel(file, it)
                        pendingResult = result
                        Log.d(TAG, "Initiating UAEPassController.signDocument...")
                        UAEPassController.signDocument(activity!!, requestModel, object : UAEPassDocumentSigningCallback {
                            override fun getDocumentUrl(spId: String?, documentURL: String?, error: String?) {
                                if (error != null) {
                                    Log.e(TAG, "Document signing failed with error: $error")
                                    Toast.makeText(activity, "Error while signing document: $error", Toast.LENGTH_SHORT).show()
                                    pendingResult?.error("SIGNING_FAILED", error, null)
                                } else if (documentURL != null) {
                                    Log.i(TAG, "Document Signed Successfully. URL: $documentURL")
                                    Toast.makeText(activity, "Document Signed Successfully", Toast.LENGTH_SHORT).show()
                                    downloadDocument(file.name, documentURL)
                                    pendingResult?.success("Document signed successfully. URL: $documentURL")
                                } else {
                                    Log.w(TAG, "Document URL not received after successful signing.")
                                    pendingResult?.error("SIGNING_FAILED", "Document URL not received.", null)
                                }
                                pendingResult = null
                            }
                        })
                    } ?: run {
                        Log.e(TAG, "Failed to load document signing parameters from JSON.")
                        result.error("INVALID_JSON", "Failed to load document signing parameters from JSON.", null)
                    }
                } else {
                    Log.e(TAG, "Missing finishCallbackUrl argument.")
                    result.error("INVALID_ARGUMENTS", "Missing finishCallbackUrl argument.", null)
                }
            }
            else -> result.notImplemented()
        }
    }

    private fun loadDocumentFromAssets(): File {
        val f = File(activity!!.filesDir, "dummy.pdf")
        try {
            val `is` = activity!!.assets.open("dummy.pdf")
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            val fos = FileOutputStream(f)
            fos.write(buffer)
            fos.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading document from assets", e)
            throw RuntimeException(e)
        }
        return f
    }

    private fun createSamplePdfFile(context: Context): File {
        val tempFile = File(context.cacheDir, "sample_doc.pdf")
        val sampleText = "This is a sample document for UAE Pass signing."
        writeTextToPdf(tempFile, sampleText)
        return tempFile
    }

    private fun writeTextToPdf(file: File, text: String) {
        val document = Document()
        try {
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()
            document.add(Paragraph(text))
        } catch (e: IOException) {
            throw RuntimeException("Error creating PDF file", e)
        } finally {
            if (document.isOpen) {
                document.close()
            }
        }
    }

    private fun loadDocumentSigningJson(finishCallbackUrl: String): DocumentSigningRequestParams? {
        return try {
            activity?.assets?.open("testSignData.json")?.use { inputStream ->
                InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader ->
                    val params = Gson().fromJson(reader, DocumentSigningRequestParams::class.java)
                    // Set the finishCallbackUrl from the method argument
                    params.finishCallbackUrl = finishCallbackUrl
                    params
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Error loading document signing JSON", ex)
            null
        }
    }

    private fun downloadDocument(fileName: String, documentUrl: String?) {
        Toast.makeText(activity, "Download functionality not implemented", Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent): Boolean {
        handleIntent(intent)
        return false
    }

    private fun handleIntent(intent: Intent?) {
        if (intent != null && intent.data != null) {
            if (scheme!! == intent.data!!.scheme) {
                UAEPassController.resume(intent.dataString)
            }
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    private fun isPackageInstalled(packageManager: PackageManager): Boolean {
        val packageName = when (environment) {
            is Environment.STAGING -> {
                UAE_PASS_STG_PACKAGE_ID
            }
            is Environment.QA -> {
                UAE_PASS_QA_PACKAGE_ID
            }
            is Environment.PRODUCTION -> {
                UAE_PASS_PACKAGE_ID
            }
            else -> {
                UAE_PASS_PACKAGE_ID
            }
        }
        var found = true
        try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            found = false
        }
        return found
    }

    private fun getAuthenticationRequestModel(context: Context): UAEPassAccessTokenRequestModel {
        val ACR_VALUE = if (isPackageInstalled(context.packageManager)) {
            ACR_VALUES_MOBILE
        } else {
            ACR_VALUES_WEB
        }
        val LANGUAGE = if (language == "ar") {
            Language.AR
        } else {
            Language.EN
        }
        return UAEPassAccessTokenRequestModel(
            environment,
            client_id!!,
            client_secret!!,
            scheme!!,
            failureHost!!,
            successHost!!,
            redirect_url!!,
            scope!!,
            RESPONSE_TYPE,
            ACR_VALUE,
            state!!,
            LANGUAGE
        )
    }

    private fun getProfileRequestModel(context: Context): UAEPassProfileRequestModel {
        val ACR_VALUE = if (isPackageInstalled(context.packageManager)) {
            ACR_VALUES_MOBILE
        } else {
            ACR_VALUES_WEB
        }
        val LANGUAGE = if (language == "ar") {
            Language.AR
        } else {
            Language.EN
        }
        return UAEPassProfileRequestModel(
            environment,
            client_id!!,
            client_secret!!,
            scheme!!,
            failureHost!!,
            successHost!!,
            redirect_url!!,
            scope!!,
            RESPONSE_TYPE,
            ACR_VALUE,
            state!!,
            LANGUAGE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == 1001) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val code = data?.getStringExtra(UAEPassWebViewActivity.RESULT_CODE_SUCCESS)
                    if (code != null) {
                        result.success(code)
                    } else {
                        result.error("ERROR", "Unable to get access code", null)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    val error = data?.getStringExtra(UAEPassWebViewActivity.RESULT_CODE_CANCELLED)
                        ?: "Authentication Process Canceled By User"
                    result.error("ERROR", error, null)
                }
                else -> {
                    result.error("ERROR", "Unknown error occurred", null)
                }
            }
            return true
        }
        return false
    }

    private fun buildAuthUrl(): String {
        val acrValue = if (isPackageInstalled(activity!!.packageManager)) {
            ACR_VALUES_MOBILE
        } else {
            ACR_VALUES_WEB
        }

        val baseUrl = when (environment) {
            Environment.PRODUCTION -> "https://id.uaepass.ae/idshub/authorize"
            else -> "https://stg-id.uaepass.ae/idshub/authorize"
        }

        return "$baseUrl?" +
                "response_type=$RESPONSE_TYPE&" +
                "client_id=$client_id&" +
                "redirect_uri=$redirect_url&" +
                "scope=$scope&" +
                "state=$state&" +
                "acr_values=$acrValue&" +
                "ui_locales=${if (language == "ar") "ar" else "en"}"
    }
}