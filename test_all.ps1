$urlBase = "http://localhost:8081"

function Test-Endpoint {
    param(
        [string]$Name,
        [scriptblock]$Script
    )
    Write-Host "[$Name] Ejecutando..."
    try {
        & $Script
        Write-Host "[$Name] OK`n" -ForegroundColor Green
    } catch {
        Write-Host "[$Name] ERROR" -ForegroundColor Red
        if ($_.Exception -and $_.Exception.Response) {
            $streamReader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $ErrResp = $streamReader.ReadToEnd()
            $streamReader.Close()
            Write-Host $ErrResp
        } else {
            Write-Host $_
        }
        Write-Host ""
    }
}

$idRegistro = ""
$token = ""
$numeroCuenta = ""
$email = "test.final@example.com"

Test-Endpoint -Name "Registro de Cliente Exitoso" -Script {
    $body = @{
        nombre = "Test Final"
        segundoNombre = "Avanzado"
        apellidoPaterno = "Perez"
        apellidoMaterno = "Gomez"
        fechaNacimiento = "1985-05-10"
        curp = "FINA850510HDFRZZ97"
        rfc = "FINA850510A1A"
        sexo = "HOMBRE"
        nacionalidad = "MEXICANA"
        estadoCivil = "SOLTERO"
        correoElectronico = $email
        telefonoMovil = "5551234567"
        ocupacion = "INGENIERO"
        empresa = "TECH"
        ingresoMensual = 15000.50
        password = "Password123!"
        calle = "Madero"
        numeroExterior = "123"
        codigoPostal = "06000"
        pais = "MEXICO"
    } | ConvertTo-Json

    $res = Invoke-RestMethod -Uri "$urlBase/clientes" -Method Post -Body $body -ContentType "application/json"
    if ($res.mensaje -notmatch "exitosamente") { throw "Registro fallido" }
}

Test-Endpoint -Name "Registro Fallido (RFC Duplicado)" -Script {
    $body = @{
        nombre = "Test Final Dos"
        apellidoPaterno = "Perez"
        apellidoMaterno = "Gomez"
        fechaNacimiento = "1985-05-10"
        curp = "OTRA850510HDFRZZ97"
        rfc = "FINA850510A1A"
        sexo = "HOMBRE"
        nacionalidad = "MEXICANA"
        estadoCivil = "SOLTERO"
        correoElectronico = "otro@example.com"
        telefonoMovil = "5551234567"
        ocupacion = "INGENIERO"
        empresa = "TECH"
        ingresoMensual = 15000.50
        password = "Password123!"
        calle = "Madero"
        numeroExterior = "123"
        codigoPostal = "06000"
        pais = "MEXICO"
    } | ConvertTo-Json

    try {
        Invoke-RestMethod -Uri "$urlBase/clientes" -Method Post -Body $body -ContentType "application/json"
        throw "Deber�a haber fallado"
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -ne 409) { throw "Status incorrecto" }
    }
}

Test-Endpoint -Name "Login Exitoso" -Script {
    $body = @{
        correo = $email
        password = "Password123!"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$urlBase/auth/login" -Method Post -Body $body -ContentType "application/json"
    $script:token = $res.token
    if (-not $script:token) { throw "No hay token" }
}

$headers = @{
    Authorization = "Bearer $token"
}

Test-Endpoint -Name "GET /clientes (Filtro Nombre)" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/clientes?nombre=Test Final" -Method Get -Headers $headers
    if ($res.Count -eq 0) { throw "No regres� datos" }
    $script:idRegistro = $res[0].id
    $script:numeroCuenta = $res[0].cuentas[0].numeroCuenta
}

Test-Endpoint -Name "GET /clientes (Filtros Varios)" -Script {
    # Filtro Segundo Nombre
    $res = Invoke-RestMethod -Uri "$urlBase/clientes?segundoNombre=Avanzado" -Method Get -Headers $headers
    if ($res.Count -eq 0) { throw "Fallo Segundo Nombre" }

    # Filtro Apellido
    $res = Invoke-RestMethod -Uri "$urlBase/clientes?apellidoPaterno=Perez" -Method Get -Headers $headers
    if ($res.Count -eq 0) { throw "Fallo Apellido Paterno" }

    # Filtro CURP
    $res = Invoke-RestMethod -Uri "$urlBase/clientes?curp=FINA850510" -Method Get -Headers $headers
    if ($res.Count -eq 0) { throw "Fallo CURP" }

    # Filtro Numero Cuenta
    $res = Invoke-RestMethod -Uri "$urlBase/clientes?numeroCuenta=$($script:numeroCuenta)" -Method Get -Headers $headers
    if ($res.Count -eq 0) { throw "Fallo Numero Cuenta" }
}

Test-Endpoint -Name "GET /clientes/{id}" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/clientes/$($script:idRegistro)" -Method Get -Headers $headers
    if ($res.id -ne $script:idRegistro) { throw "ID no coincide" }
}

Test-Endpoint -Name "PATCH /clientes/{id}" -Script {
    $body = @{
        segundoNombre = "Modificado"
        ocupacion = "ARQUITECTO"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$urlBase/clientes/$($script:idRegistro)" -Method Patch -Body $body -ContentType "application/json" -Headers $headers
    if ($res.segundoNombre -ne "Modificado") { throw "No actualiz� segundo nombre" }
}

Test-Endpoint -Name "POST /cuentas" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/cuentas?clienteId=$($script:idRegistro)" -Method Post -Headers $headers
    if (-not $res.numeroCuenta) { throw "No se cre� la cuenta" }
}

Test-Endpoint -Name "GET /cuentas/{numeroCuenta}" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/cuentas/$($script:numeroCuenta)" -Method Get -Headers $headers
    if ($res.numeroCuenta -ne $script:numeroCuenta) { throw "N�mero de cuenta no coincide" }
}

Test-Endpoint -Name "PATCH /cuentas/{numeroCuenta}" -Script {
    $body = @{
        saldo = 5000.00
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$urlBase/cuentas/$($script:numeroCuenta)" -Method Patch -Body $body -ContentType "application/json" -Headers $headers
    if ($res.saldo -ne 5000.00) { throw "No actualiz� saldo" }
}

Test-Endpoint -Name "GET /cuentas?estatus=ACTIVA" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/cuentas?estatus=ACTIVA" -Method Get -Headers $headers
    if ($res.Count -eq 0) { throw "No hay cuentas activas" }
}

Test-Endpoint -Name "GET /usuarios/filtro" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/usuarios/filtro?correo=$email" -Method Get -Headers $headers
    if ($res.Count -eq 0) { throw "No se encontr� usuario" }
}

Test-Endpoint -Name "DELETE /clientes/{id}" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/clientes/$($script:idRegistro)" -Method Delete -Headers $headers
    if ($res.mensaje -notmatch "desactivado exitosamente") { throw "No se desactiv�" }
}

Test-Endpoint -Name "GET /cuentas?estatus=INACTIVA (Verificaci�n de baja en cascada)" -Script {
    $res = Invoke-RestMethod -Uri "$urlBase/cuentas/$($script:numeroCuenta)" -Method Get -Headers $headers
    if ($res.estatus -ne "INACTIVA") { throw "La cuenta no se desactiv�" }
}

Test-Endpoint -Name "POST /clientes/reactivar" -Script {
    $body = @{
        correo = $email
        password = "Password123!"
        curp = "FINA850510HDFRZZ97"
        rfc = "FINA850510A1A"
        fechaNacimiento = "1985-05-10"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$urlBase/clientes/reactivar" -Method Post -Body $body -ContentType "application/json"
    if ($res.mensaje -notmatch "reactivada exitosamente") { throw "No se reactiv�" }
}

