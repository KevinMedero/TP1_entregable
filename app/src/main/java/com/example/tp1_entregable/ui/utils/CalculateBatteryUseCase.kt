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
    val percentage: Int,                // Porcentaje actual de la batería
    val formattedTimeToEmpty: String,   // Hora exacta estimada en la que se agotará
    val hoursRemainingText: String      // Tiempo restante de duración en horas y minutos
)

// Caso de uso para calcular el nivel actual de la batería y proyectar el tiempo restante
class CalculateBatteryUseCase {

    // Sobrecarga del operador invoke para permitir invocar la clase como una función: calculateBatteryUseCase(context)
    operator fun invoke(context: Context): BatteryInfo {
        // Obtiene el estado actual de la batería
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, iFilter)

        // Lee los valores de nivel actual y escala máxima desde los datos extra del sistema
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        // Calcula el porcentaje de batería actual
        val pct = if (level != -1 && scale != -1) {
            (level / scale.toFloat() * 100).toInt()
        } else {
            50  // Valor de respaldo por defecto si la lectura falla
        }

        // Estimación de consumo
        val consumptionRatePerHour = 12.0f
        val hoursLeft = pct / consumptionRatePerHour
        val hoursInt = hoursLeft.toInt()
        val minutesInt = ((hoursLeft - hoursInt) * 60).toInt()

        // Calcula la hora exacta en la que la batería alcanzará el 0% sumando los minutos proyectados al reloj actual
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, (hoursLeft * 60).toInt())
        }
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        // Construye y retorna la instancia con la información formateada
        return BatteryInfo(
            percentage = pct,
            formattedTimeToEmpty = "${format.format(calendar.time)} hs",
            hoursRemainingText = "${hoursInt}h ${minutesInt}m"
        )
    }
}