package com.proyect.tradersroom

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.proyect.tradersroom.model.remote.BitacoraRemote
import com.proyect.tradersroom.model.remote.UsuarioRemote
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_registro.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern


class RegistroActivity : AppCompatActivity() {

    private lateinit var fecha: String
    private var cal = Calendar.getInstance()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val format = "MM/dd/yyyy"
                val simpleDateFormat = SimpleDateFormat(format, Locale.US)
                fecha = simpleDateFormat.format(cal.time).toString()
                tv_date.text = fecha
            }
        }

        ib_calendario.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        //AQUI EMPIEZA FIREBASE
        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

        bt_registrar.setOnClickListener {

            val nombre = et_nombre.text.toString()
            val telefono = et_telefono.text.toString()
            val correo = et_correo.text.toString()
            val contrasena = et_contrasena.text.toString()
            val rep_contrasena = et_contrasena2.text.toString()
            var roll = sp_roll.selectedItem.toString()

            val passLength: Int = stringLengthFunc("$contrasena")

            validarRegistro(
                nombre,
                telefono,
                correo,
                contrasena,
                passLength,
                rep_contrasena,
                mAuth,
                roll
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun validarRegistro(
        nombre: String,
        telefono: String,
        correo: String,
        contrasena: String,
        passLength: Int,
        rep_contrasena: String,
        mAuth: FirebaseAuth,
        roll: String
    ) {
        if (nombre.isEmpty()) { //NOMBRE VACIO
            et_nombre.error = "Por favor ingrese su nombre"
            et_nombre.requestFocus()
        } else if (telefono.isEmpty()) { //TELEFONO VACIO
            et_telefono.error = "Por favor ingrese su numero telefonico"
            et_telefono.requestFocus()
        } else if (correo.isEmpty()) { //CORREO VACIO
            et_correo.error = "Por favor ingrese su correo"
            et_correo.requestFocus()
        } else if (!validarEmail(correo)) {
            et_correo.setError("Email no válido")
            et_correo.requestFocus()
        } else if (contrasena.isEmpty()) { //CONTRASEÑA VACIA
            et_contrasena.error = "Por favor ingrese una contraseña"
            et_contrasena.requestFocus()
        } else if (passLength < 6) { //CONTRASEÑA DE 6 DIGITOS
            et_contrasena.error = "La contraseña debe tener minimo 6 caracteres"
            et_contrasena.requestFocus()
        } else if (rep_contrasena.isEmpty()) { //CONTRASEÑA 2 VACIA
            et_contrasena2.error = "Por favor repita su contraseña"
            et_contrasena2.requestFocus()
        } else if (contrasena != rep_contrasena) { //CONTRASEÑAS DIFERENTES
            et_contrasena2.error = "Las contraseñas no coinciden"
            et_contrasena2.requestFocus()
        } else if (tv_date.text.toString().isEmpty()) { // FECHA VACIA
            tv_date.error = "Por favor ingrese su fecha de nacimiento"
            tv_date.requestFocus()
            tv_resultado.text = "Por favor ingrese su fecha de nacimiento"
        } else if (tv_date.text.toString() == "MM/dd/yyyy") { //FECHA INCORRECTA
            tv_date.error = "Por favor ingrese su fecha de nacimiento"
            tv_date.requestFocus()
            tv_resultado.text = "Por favor ingrese su fecha de nacimiento"
        } else {
            mAuth.createUserWithEmailAndPassword(correo, contrasena)
                .addOnCompleteListener(
                    this
                ) { task ->
                    if (task.isSuccessful) {

                        crearUsuarioEnFirebase(nombre, telefono, correo, roll)
                        goToLogin()

                    } else {
                        Toast.makeText(this, task.getException()?.message, Toast.LENGTH_SHORT)
                            .show()
                        val exc = task.getException()?.localizedMessage
                        verificarCodigosError(exc)
                    }
                }
        }
    }

    private fun goToLogin() {
        Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun crearUsuarioEnFirebase(
        nombre: String,
        telefono: String,
        correo: String,
        roll: String
    ) {
        val database: FirebaseDatabase = FirebaseDatabase.getInstance()
        val myRef: DatabaseReference = database.getReference("usuarios")

        val id = myRef.push().key

        val usuario = UsuarioRemote(
            id,
            nombre,
            telefono,
            correo,
            fecha,
            roll,
            "https://url2.cl/cjv4I"
        )

        myRef.child(id!!).setValue(usuario)

        crearBaseDeDatos(database, id)
    }

    private fun crearBaseDeDatos(
        database: FirebaseDatabase,
        id: String?
    ) {
        val myRef2: DatabaseReference = database.getReference("bitacora").child("$id")
        val bitacora = BitacoraRemote(
            "0",
            "0",
            "0",
            "0",
            "0",
            "0",
            "0",
            "0",
            "0"
        )
        myRef2.child("0").setValue(bitacora)
    }

    val stringLengthFunc: (String) -> Int = { input ->
        input.length
    }

    private fun validarEmail(email: String): Boolean {
        val pattern: Pattern = Patterns.EMAIL_ADDRESS
        return pattern.matcher(email).matches()
    }

    private fun verificarCodigosError(exc: String?) {
        if (exc == "The email address is badly formatted.") {
            et_correo.setError("Formato invalido")
            et_correo.requestFocus()
        } else if (exc == "There is no user record corresponding to this identifier. The user may have been deleted.") {
            et_correo.setError("Usuario no existe")
            et_correo.requestFocus()
        } else if (exc == "The password is invalid or the user does not have a password.") {
            et_contrasena.setError("Contraseña incorrecta")
            et_contrasena.requestFocus()
        } else if (exc == "The email address is already in use by another account.") {
            et_correo.setError("Usuario ya registrado")
        } else if (exc == "The user account has been disabled by an administrator.") {
            et_correo.setError("Cuenta desabilitada")
        } else if (exc == "There is no user record corresponding to this identifier. The user may have been deleted.") {
            et_correo.setError("Usuario no existe")
        } else if (exc == "The given password is invalid.") {
            et_correo.setError("Contraseña incorrecta")
        } else if (exc == "The password is invalid it must 6 characters at least.") {
            et_correo.setError("Minimo 6 caracteres")
        } else {
            et_contrasena.setError("Contraseña incorrecta")
            et_contrasena.setError("Contraseña incorrecta")
            et_contrasena.requestFocus()
        }
    }

}