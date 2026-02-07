Connect Wereables documentation
================
# Health Connect Steps

1. Add Health Connect dependency
2. HealthConnectRepository. Provide functions to:
   2.1 Check Health Connect availability
   2.2 Check if permissions granted
   2.3 Get records for a type (Steps, HeartRate, Sleep, etc.)
      a) Get latest value for a type
      b) Get aggregated data for a type (e.g. total steps per day)
3. Request Health Connect permissions
4. Read data
5. (Optional depending on data type) Save lastSyncTime so the next time get from lastSyncTime to now.
   (or just bring all the data from the day)
6. (Optional) Periodic WorkManager to refresh data.
7. Privacity and Security
   7.1 Only get necessary data.
   7.2 Encrypt local data if apply.
   7.3 Secure HTTPS to send to backend



# Opciones
1. Health Connect ->
   PROS:
      Para todo android y para todos los dispositivos
   CONTRAS:
      No es tiempo real
      No se puede acceder a mas de 30 dias antes
2. Samsung Health ->
   PROS:
   Integración con Health Connect (bidireccional)
   Métricas “avanzadas” en Galaxy Watch
   Ofrece Samsung Health vale sin Samsung, pero lo más “clínico” de Samsung (ECG / presión arterial) suele requerir móvil Samsung.
   CONTRAS:
3. BLE -> 
   PROS:
      Conectar con dispositivos que no comunican sus datos a health connect: Tensiómetro, Glucómetro, Oxímetro, Termómetro
      Acceso a datos real-time (BPMs, sensores...)
      No necesitas apps de terceros (health connect o fabricante)
      Soporta dispositivos genéricos (sin marca)
   CONTRAS:
      Cada dispositivo puede requerir implementación distinta
      escaneo, pairing, reconexión y manejo de errores de radio.
      Conexiones largas -> Foreground service
4. APIs de fabricante -> mas metricas/precision pero necesidad de hacer la integracion para cada uno.
5. Wear OS “on-device”: tu app corre en el reloj (Health Services/sensores) y sincroniza al móvil/backend (no es BLE directo, es stack Wear OS).