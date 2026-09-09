package com.example.tp1_entregable.ui.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Estructura de datos para devolver el resultado
data class BatteryInfo(
    val percentage: Int,
    val formattedTimeToEmpty: String,
    val hoursRemainingText: String
)

class CalculateBatteryUseCase {
    operator fun invoke(context: Context): BatteryInfo {
        // Obtener la intención del estado de batería del sistema
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)

        // Leer nivel y escala
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        // Calcular porcentaje actual
        val pct = if (level != -1 && scale != -1) {
            (level / scale.toFloat() * 100).toInt()
        } else {
            50
        }

        // Estimación de consumo (Ejemplo: 12% por hora de uso promedio)
        val consumptionRatePerHour = 12.0f
        val hoursLeft = pct / consumptionRatePerHour
        val hoursInt = hoursLeft.toInt()
        val minutesInt = ((hoursLeft - hoursInt) * 60).toInt()

        // Calcular la hora proyectada de apagado
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, (hoursLeft * 60).toInt())
        }
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        return BatteryInfo(
            percentage = pct,
            formattedTimeToEmpty = "${format.format(calendar.time)} hs",
            hoursRemainingText = "${hoursInt}h ${minutesInt}m"
        )
    }
}