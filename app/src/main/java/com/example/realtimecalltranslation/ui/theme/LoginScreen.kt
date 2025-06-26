package com.example.realtimecalltranslation.ui.theme

import android.app.Activity
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.realtimecalltranslation.R
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.android.gms.tasks.Task // Uncommented for explicit type
import com.google.firebase.auth.AuthResult // Uncommented for explicit type
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Define colors (can be moved to a Colors.kt or Theme.kt later)
// These color vals seem to be defined globally or in another theme file,
// if not, they should be defined here or imported.
// For now, assuming they are accessible or will be defined.
// val mainRed = Color(0xFFD32F2F) // Example Red - Assuming this is defined elsewhere or in Theme.kt
// val lightRed = Color(0xFFFFCDD2) // Example Light Red - Assuming this is defined elsewhere or in Theme.kt
// val mainWhite = Color.White - This is standard.
// val darkerRed = Color(0xFFB71C1C) - Assuming this is defined elsewhere or in Theme.kt

// Define colors based on user's new specification
val LoginPrimaryRed = Color(0xFF8B0000) // Dark Red / Maroon
val LoginBackgroundWhite = Color.White // Standard White
val LoginInputBackgroundGray = Color(0xFFF5F5F5)
val LoginAccentGolden = Color(0xFFFFD700)
val LoginErrorRed = Color(0xFFFF4C4C)
val LoginSuccessGreen = Color(0xFF4CAF50)
val LoginBorderColor = Color(0xFFCCCCCC) // Added from previous plan
val LoginFocusedBorderColor = LoginPrimaryRed // Added from previous plan

data class CountryData(val name: String, val phoneCode: String, val isoCode: String)

val countryList = listOf(
    CountryData("Afghanistan", "+93", "AF"),
    CountryData("Albania", "+355", "AL"),
    CountryData("Algeria", "+213", "DZ"),
    CountryData("Andorra", "+376", "AD"),
    CountryData("Angola", "+244", "AO"),
    CountryData("Argentina", "+54", "AR"),
    CountryData("Armenia", "+374", "AM"),
    CountryData("Australia", "+61", "AU"),
    CountryData("Austria", "+43", "AT"),
    CountryData("Azerbaijan", "+994", "AZ"),
    CountryData("Bahrain", "+973", "BH"),
    CountryData("Bangladesh", "+880", "BD"),
    CountryData("Belarus", "+375", "BY"),
    CountryData("Belgium", "+32", "BE"),
    CountryData("Belize", "+501", "BZ"),
    CountryData("Benin", "+229", "BJ"),
    CountryData("Bhutan", "+975", "BT"),
    CountryData("Bolivia", "+591", "BO"),
    CountryData("Bosnia & Herzegovina", "+387", "BA"),
    CountryData("Botswana", "+267", "BW"),
    CountryData("Brazil", "+55", "BR"),
    CountryData("Brunei", "+673", "BN"),
    CountryData("Bulgaria", "+359", "BG"),
    CountryData("Burkina Faso", "+226", "BF"),
    CountryData("Burundi", "+257", "BI"),
    CountryData("Cambodia", "+855", "KH"),
    CountryData("Cameroon", "+237", "CM"),
    CountryData("Canada", "+1", "CA"),
    CountryData("Cape Verde", "+238", "CV"),
    CountryData("Central African Rep.", "+236", "CF"),
    CountryData("Chad", "+235", "TD"),
    CountryData("Chile", "+56", "CL"),
    CountryData("China", "+86", "CN"),
    CountryData("Colombia", "+57", "CO"),
    CountryData("Comoros", "+269", "KM"),
    CountryData("Congo (Brazzaville)", "+242", "CG"),
    CountryData("Congo (Kinshasa)", "+243", "CD"),
    CountryData("Costa Rica", "+506", "CR"),
    CountryData("Croatia", "+385", "HR"),
    CountryData("Cuba", "+53", "CU"),
    CountryData("Cyprus", "+357", "CY"),
    CountryData("Czech Republic", "+420", "CZ"),
    CountryData("Denmark", "+45", "DK"),
    CountryData("Djibouti", "+253", "DJ"),
    CountryData("Dominica", "+1-767", "DM"),
    CountryData("Dominican Republic", "+1-809", "DO"),
    CountryData("Ecuador", "+593", "EC"),
    CountryData("Egypt", "+20", "EG"),
    CountryData("El Salvador", "+503", "SV"),
    CountryData("Equatorial Guinea", "+240", "GQ"),
    CountryData("Eritrea", "+291", "ER"),
    CountryData("Estonia", "+372", "EE"),
    CountryData("Eswatini (Swaziland)", "+268", "SZ"),
    CountryData("Ethiopia", "+251", "ET"),
    CountryData("Fiji", "+679", "FJ"),
    CountryData("Finland", "+358", "FI"),
    CountryData("France", "+33", "FR"),
    CountryData("Gabon", "+241", "GA"),
    CountryData("Gambia", "+220", "GM"),
    CountryData("Georgia", "+995", "GE"),
    CountryData("Germany", "+49", "DE"),
    CountryData("Ghana", "+233", "GH"),
    CountryData("Greece", "+30", "GR"),
    CountryData("Grenada", "+1-473", "GD"),
    CountryData("Guatemala", "+502", "GT"),
    CountryData("Guinea", "+224", "GN"),
    CountryData("Guinea-Bissau", "+245", "GW"),
    CountryData("Guyana", "+592", "GY"),
    CountryData("Haiti", "+509", "HT"),
    CountryData("Honduras", "+504", "HN"),
    CountryData("Hungary", "+36", "HU"),
    CountryData("Iceland", "+354", "IS"),
    CountryData("India", "+91", "IN"),
    CountryData("Indonesia", "+62", "ID"),
    CountryData("Iran", "+98", "IR"),
    CountryData("Iraq", "+964", "IQ"),
    CountryData("Ireland", "+353", "IE"),
    CountryData("Israel", "+972", "IL"),
    CountryData("Italy", "+39", "IT"),
    CountryData("Ivory Coast", "+225", "CI"),
    CountryData("Jamaica", "+1-876", "JM"),
    CountryData("Japan", "+81", "JP"),
    CountryData("Jordan", "+962", "JO"),
    CountryData("Kazakhstan", "+7", "KZ"),
    CountryData("Kenya", "+254", "KE"),
    CountryData("Kiribati", "+686", "KI"),
    CountryData("Korea (North)", "+850", "KP"),
    CountryData("Korea (South)", "+82", "KR"),
    CountryData("Kuwait", "+965", "KW"),
    CountryData("Kyrgyzstan", "+996", "KG"),
    CountryData("Laos", "+856", "LA"),
    CountryData("Latvia", "+371", "LV"),
    CountryData("Lebanon", "+961", "LB"),
    CountryData("Lesotho", "+266", "LS"),
    CountryData("Liberia", "+231", "LR"),
    CountryData("Libya", "+218", "LY"),
    CountryData("Liechtenstein", "+423", "LI"),
    CountryData("Lithuania", "+370", "LT"),
    CountryData("Luxembourg", "+352", "LU"),
    CountryData("Madagascar", "+261", "MG"),
    CountryData("Malawi", "+265", "MW"),
    CountryData("Malaysia", "+60", "MY"),
    CountryData("Maldives", "+960", "MV"),
    CountryData("Mali", "+223", "ML"),
    CountryData("Malta", "+356", "MT"),
    CountryData("Marshall Islands", "+692", "MH"),
    CountryData("Mauritania", "+222", "MR"),
    CountryData("Mauritius", "+230", "MU"),
    CountryData("Mexico", "+52", "MX"),
    CountryData("Micronesia", "+691", "FM"),
    CountryData("Moldova", "+373", "MD"),
    CountryData("Monaco", "+377", "MC"),
    CountryData("Mongolia", "+976", "MN"),
    CountryData("Montenegro", "+382", "ME"),
    CountryData("Morocco", "+212", "MA"),
    CountryData("Mozambique", "+258", "MZ"),
    CountryData("Myanmar (Burma)", "+95", "MM"),
    CountryData("Namibia", "+264", "NA"),
    CountryData("Nauru", "+674", "NR"),
    CountryData("Nepal", "+977", "NP"),
    CountryData("Netherlands", "+31", "NL"),
    CountryData("New Zealand", "+64", "NZ"),
    CountryData("Nicaragua", "+505", "NI"),
    CountryData("Niger", "+227", "NE"),
    CountryData("Nigeria", "+234", "NG"),
    CountryData("North Macedonia", "+389", "MK"),
    CountryData("Norway", "+47", "NO"),
    CountryData("Oman", "+968", "OM"),
    CountryData("Pakistan", "+92", "PK"),
    CountryData("Palau", "+680", "PW"),
    CountryData("Palestine", "+970", "PS"),
    CountryData("Panama", "+507", "PA"),
    CountryData("Papua New Guinea", "+675", "PG"),
    CountryData("Paraguay", "+595", "PY"),
    CountryData("Peru", "+51", "PE"),
    CountryData("Philippines", "+63", "PH"),
    CountryData("Poland", "+48", "PL"),
    CountryData("Portugal", "+351", "PT"),
    CountryData("Qatar", "+974", "QA"),
    CountryData("Romania", "+40", "RO"),
    CountryData("Russia", "+7", "RU"),
    CountryData("Rwanda", "+250", "RW"),
    CountryData("Saint Kitts & Nevis", "+1-869", "KN"),
    CountryData("Saint Lucia", "+1-758", "LC"),
    CountryData("Saint Vincent", "+1-784", "VC"),
    CountryData("Samoa", "+685", "WS"),
    CountryData("San Marino", "+378", "SM"),
    CountryData("Saudi Arabia", "+966", "SA"),
    CountryData("Senegal", "+221", "SN"),
    CountryData("Serbia", "+381", "RS"),
    CountryData("Seychelles", "+248", "SC"),
    CountryData("Sierra Leone", "+232", "SL"),
    CountryData("Singapore", "+65", "SG"),
    CountryData("Slovakia", "+421", "SK"),
    CountryData("Slovenia", "+386", "SI"),
    CountryData("Solomon Islands", "+677", "SB"),
    CountryData("Somalia", "+252", "SO"),
    CountryData("South Africa", "+27", "ZA"),
    CountryData("South Sudan", "+211", "SS"),
    CountryData("Spain", "+34", "ES"),
    CountryData("Sri Lanka", "+94", "LK"),
    CountryData("Sudan", "+249", "SD"),
    CountryData("Suriname", "+597", "SR"),
    CountryData("Sweden", "+46", "SE"),
    CountryData("Switzerland", "+41", "CH"),
    CountryData("Syria", "+963", "SY"),
    CountryData("Taiwan", "+886", "TW"),
    CountryData("Tajikistan", "+992", "TJ"),
    CountryData("Tanzania", "+255", "TZ"),
    CountryData("Thailand", "+66", "TH"),
    CountryData("Timor-Leste", "+670", "TL"),
    CountryData("Togo", "+228", "TG"),
    CountryData("Tonga", "+676", "TO"),
    CountryData("Trinidad & Tobago", "+1-868", "TT"),
    CountryData("Tunisia", "+216", "TN"),
    CountryData("Turkey", "+90", "TR"),
    CountryData("Turkmenistan", "+993", "TM"),
    CountryData("Tuvalu", "+688", "TV"),
    CountryData("Uganda", "+256", "UG"),
    CountryData("Ukraine", "+380", "UA"),
    CountryData("United Arab Emirates", "+971", "AE"),
    CountryData("United Kingdom", "+44", "GB"),
    CountryData("United States", "+1", "US"),
    CountryData("Uruguay", "+598", "UY"),
    CountryData("Uzbekistan", "+998", "UZ"),
    CountryData("Vanuatu", "+678", "VU"),
    CountryData("Vatican City", "+379", "VA"),
    CountryData("Venezuela", "+58", "VE"),
    CountryData("Vietnam", "+84", "VN"),
    CountryData("Yemen", "+967", "YE"),
    CountryData("Zambia", "+260", "ZM"),
    CountryData("Zimbabwe", "+263", "ZW")
).sortedBy { it.name }

@OptIn(ExperimentalMaterial3Api::class) // Required for ExposedDropdownMenuBox
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {}, // Renamed for clarity
    // Other callbacks like onFacebook, onGoogle, onSignUp are removed
    // Firebase Auth logic will be handled internally or via a ViewModel
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var verificationIdState by remember { mutableStateOf<String?>(null) }
    var forceResendingTokenState by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }

    val countries = remember {
        listOf(
            CountryData("Afghanistan", "+93", "AF"),
            CountryData("Bangladesh", "+880", "BD"),
            CountryData("India", "+91", "IN"),
            CountryData("Pakistan", "+92", "PK"),
            CountryData("United States", "+1", "US"),
            CountryData("United Kingdom", "+44", "GB")
            // Add full list later or load from resources
        )
    }
    var countryPickerExpanded by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(countries.firstOrNull { it.phoneCode == "+880" } ?: countries.first()) }


    val context = LocalContext.current
    val activity = LocalContext.current as? Activity // Safely cast to Activity
    val auth = FirebaseAuth.getInstance()

    val coroutineScope = rememberCoroutineScope()

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("LoginScreenAuth", "onVerificationCompleted: Auto-retrieval or instant verification. Credential: $credential")
            isLoading = true
            errorMessage = null
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i("LoginScreenAuth", "signInWithCredential (onVerificationCompleted) successful.")
                        onLoginSuccess()
                    } else {
                        Log.w("LoginScreenAuth", "signInWithCredential (onVerificationCompleted) failed.", task.exception)
                        errorMessage = "Auto-verification failed: ${task.exception?.message ?: "Unknown error"}"
                    }
                    isLoading = false
                }
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("LoginScreenAuth", "onVerificationFailed: ", e)
            errorMessage = "OTP Verification Failed: ${e.message}"
            isLoading = false
            otpSent = false // Allow user to try sending OTP again
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d("LoginScreenAuth", "onCodeSent: Verification ID: $verificationId, Token: $token")
            verificationIdState = verificationId
            forceResendingTokenState = token
            otpSent = true // Move to OTP input screen
            isLoading = false
            errorMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LoginBackgroundWhite) // Using new background color
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            // verticalArrangement = Arrangement.Center // Will adjust spacing manually
        ) {
            Spacer(modifier = Modifier.weight(0.1f)) // Pushes content down a bit

            // Top Section: Logo and App Name
            Image(
                painter = painterResource(id = R.drawable.ic_login_illustration), // Replace with your actual logo
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp) // Adjusted size
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "RealTimeCallTranslation",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp, // Adjusted size
                color = LoginPrimaryRed // Using new primary color
            )
            Text(
                "Talk Freely, Understand Clearly.",
                fontSize = 14.sp, // Adjusted size
                color = LoginPrimaryRed.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(0.1f)) // Spacer

            // Middle Section: Inputs and Buttons
            Surface( // Card like appearance for input area
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                color = LoginBackgroundWhite // Card background
            ) {
                Column(
                    modifier = Modifier.padding(all = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!otpSent) {
                        Text("Enter Phone Number", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = LoginPrimaryRed)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExposedDropdownMenuBox(
                                expanded = countryPickerExpanded,
                                onExpandedChange = { countryPickerExpanded = !countryPickerExpanded },
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                OutlinedTextField(
                                    value = "${selectedCountry.isoCode} ${selectedCountry.phoneCode}",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryPickerExpanded) },
                                    modifier = Modifier.menuAnchor().width(130.dp), // Prevent clickable box from expanding too much
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LoginPrimaryRed,
                                        unfocusedBorderColor = LoginPrimaryRed.copy(alpha = 0.5f),
                                        disabledTextColor = LoginPrimaryRed, // Color when readOnly
                                        disabledBorderColor = LoginPrimaryRed.copy(alpha = 0.5f),
                                        disabledLabelColor = LoginPrimaryRed.copy(alpha = 0.7f),
                                        disabledTrailingIconColor = LoginPrimaryRed
                                    )
                                )
                                ExposedDropdownMenu(
                                    expanded = countryPickerExpanded,
                                    onDismissRequest = { countryPickerExpanded = false }
                                ) {
                                    countries.forEach { country ->
                                        DropdownMenuItem(
                                            text = { Text("${country.name} (${country.phoneCode})") },
                                            onClick = {
                                                selectedCountry = country
                                                countryPickerExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it.filter { char -> char.isDigit() } },
                                label = { Text("Local Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LoginPrimaryRed,
                                    unfocusedBorderColor = LoginPrimaryRed.copy(alpha = 0.5f),
                                    cursorColor = LoginPrimaryRed,
                                    focusedLabelColor = LoginPrimaryRed,
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (activity == null) {
                                    errorMessage = "Error: Activity context not available."
                                    return@Button
                                }
                                if (phoneNumber.isBlank()) { // Basic validation, length check can be more specific
                                    errorMessage = "Please enter your local phone number."
                                    return@Button
                                }
                                isLoading = true; errorMessage = null
                                val fullPhoneNumber = selectedCountry.phoneCode + phoneNumber
                                Log.d("LoginScreenAuth", "Sending OTP to: $fullPhoneNumber")
                                val options = PhoneAuthOptions.newBuilder(auth)
                                    .setPhoneNumber(fullPhoneNumber)
                                    .setTimeout(60L, TimeUnit.SECONDS)
                                    .setActivity(activity)
                                    .setCallbacks(callbacks)
                                    .build()
                                PhoneAuthProvider.verifyPhoneNumber(options)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LoginPrimaryRed),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LoginBackgroundWhite)
                            else Text("Send OTP", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LoginBackgroundWhite)
                        }
                    } else { // OTP Input Section
                        Text("Enter OTP", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = LoginPrimaryRed)
                        Text("Sent to +880$phoneNumber", fontSize = 14.sp, color = LoginPrimaryRed.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { if (it.length <= 6) otp = it.filter { char -> char.isDigit() } },
                            label = { Text("6-Digit OTP") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LoginPrimaryRed,
                                unfocusedBorderColor = LoginPrimaryRed.copy(alpha = 0.5f),
                                cursorColor = LoginPrimaryRed,
                                focusedLabelColor = LoginPrimaryRed
                            )
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                if (verificationIdState == null) {
                                    errorMessage = "Verification ID missing. Try again."
                                    return@Button
                                }
                                if (otp.length != 6) {
                                    errorMessage = "Enter a valid 6-digit OTP."
                                    return@Button
                                }
                                isLoading = true; errorMessage = null
                                val credential = PhoneAuthProvider.getCredential(verificationIdState!!, otp)
                                auth.signInWithCredential(credential)
                                    .addOnCompleteListener(activity!!) { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            onLoginSuccess()
                                        } else {
                                            errorMessage = "OTP Verification Failed: ${task.exception?.message ?: "Invalid OTP"}"
                                        }
                                    }
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LoginAccentGolden), // Using Accent color
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = LoginPrimaryRed)
                            else Text("Verify & Proceed", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LoginPrimaryRed)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(onClick = { /* TODO: Implement Resend OTP */ otpSent = false; errorMessage = null; isLoading = false }) {
                            Text("Didn't receive code? Resend / Change Number", color = LoginPrimaryRed.copy(alpha = 0.8f))
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.weight(0.2f)) // Pushes bottom content up

            // Bottom Section: Terms/Privacy (Placeholder)
            if (!otpSent) { // Show only on phone number entry screen
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "By continuing, you agree to our",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Row {
                        Text(
                            "Terms of Service",
                            fontSize = 12.sp,
                            color = LoginPrimaryRed,
                            modifier = Modifier.clickable { /* TODO */ }
                        )
                        Text(" & ", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            "Privacy Policy",
                            fontSize = 12.sp,
                            color = LoginPrimaryRed,
                            modifier = Modifier.clickable { /* TODO */ }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp)) // Final bottom padding
        }
    }
}

// Placeholder for UpgradePlanButton if it was defined elsewhere and needs to be removed
// If it's a local composable in this file, it would have been removed with the old LoginScreen content.
// @Composable
// fun UpgradePlanButton() { /* ... */ }

// Duplicate color and data class definitions at the end of the file are removed.
// The main LoginScreen function and its helper definitions (colors, CountryData, countryList)
// are kept at the top of the file.

// Placeholder for UpgradePlanButton if it was defined elsewhere and needs to be removed
// If it's a local composable in this file, it would have been removed with the old LoginScreen content.
// @Composable
// fun UpgradePlanButton() { /* ... */ }