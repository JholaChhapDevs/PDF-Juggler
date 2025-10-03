import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jholachhapdevs.pdfjuggler.ui.pdf.PdfViewModel
import com.jholachhapdevs.pdfjuggler.ui.pdf.PdfScreen


@Composable
fun App() {

    MaterialTheme {
        val viewModel = remember{ PdfViewModel().apply { newHomeTab() } }
        PdfScreen(viewModel)
    }
}


