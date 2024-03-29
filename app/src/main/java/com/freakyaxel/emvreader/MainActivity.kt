package com.freakyaxel.emvreader

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.freakyaxel.emvparser.api.CardData
import com.freakyaxel.emvparser.api.CardDataResponse
import com.freakyaxel.emvparser.api.EMVReader
import com.freakyaxel.emvparser.api.EMVReaderLogger
import com.freakyaxel.emvparser.api.fold
import com.freakyaxel.emvreader.ui.theme.EMVReaderTheme

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback, EMVReaderLogger {

    private val cardStateLabel = mutableStateOf("Tap Card to read")

    private val emvReader = EMVReader.get(this)

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            EMVReaderTheme {
                CardDataScreen(data = cardStateLabel.value)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(
            this,
            this,
            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                    NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_BARCODE or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    override fun emvLog(key: String, value: String) {
        Log.e(key, value)
    }

    override fun onTagDiscovered(tag: Tag) {
        cardStateLabel.value = "Reading Card..."

        val cardTag = EmvCardTag.get(tag)
        val cardData = emvReader.getCardData(cardTag)

        cardStateLabel.value = cardData.fold(
            onError = { it.error.message },
            onSuccess = { getCardLabel(it.cardData) },
            onTagLost = { "Card lost. Keep card steady!" },
            onCardNotSupported = { getCardNotSupportedLabel(it) }
        )
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }
}

private fun getCardNotSupportedLabel(response: CardDataResponse.CardNotSupported): String {
    val aids = response.aids
    return """
        Card is not supported!
        AID: ${aids.takeIf { it.isNotEmpty() }?.joinToString(" | ") ?: "NOT FOUND"}
    """.trimIndent()
}

private fun getCardLabel(cardData: CardData?): String {
    return """
        AID: ${cardData?.aid?.joinToString(" | ")}
        Number: ${cardData?.formattedNumber}
        Expires: ${cardData?.formattedExpDate}
    """.trimIndent()
}

@Composable
fun CardDataScreen(data: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = data)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    EMVReaderTheme {
        CardDataScreen(data = getCardLabel(null))
    }
}