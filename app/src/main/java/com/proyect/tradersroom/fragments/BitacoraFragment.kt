package com.proyect.tradersroom.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.proyect.tradersroom.BottomNavigationActivity
import com.proyect.tradersroom.R
import com.proyect.tradersroom.ResumenActivity
import com.proyect.tradersroom.model.remote.BitacoraRemote
import com.proyect.tradersroom.model.remote.UsuarioRemote
import kotlinx.android.synthetic.main.cuadro_dialogo.*
import kotlinx.android.synthetic.main.fragment_bitacora.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*


class BitacoraFragment : Fragment() {

    var maxId = 0                                           //Ultimo ID de la bitacora
    private lateinit var fecha: String                      //Fecha de operacion
    private lateinit var id1: String                        //ID del usuario
    private lateinit var capital_user: String               //Capital del usuario
    private var cal = Calendar.getInstance()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_bitacora, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()

        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker?, year: Int, month: Int, dayOfMonth: Int) {
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                val format = "MM/dd/yyyy"
                val simpleDateFormat = SimpleDateFormat(format, Locale.US)
                fecha = simpleDateFormat.format(cal.time).toString()
                tv_fecha.text = fecha
            }
        }

        //Inflar calendario
        ib_calendario2.setOnClickListener {
            DatePickerDialog(
                requireContext (),
                dateSetListener,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        //Ir al resumen
        bt_resumen.setOnClickListener {
            //findNavController().navigate(R.id.action_nav_bitacora_to_resumenFragment)
            val intent = Intent(requireContext(), ResumenActivity::class.java)
            startActivity(intent)
        }

        //Ingresar capital inicial
        bt_capital.setOnClickListener {
            cuadroDialogo()
        }

        //Inicializaciones
        capital_user = "0"
        rula()

        //Guardar en bitacora
        bt_guardar.setOnClickListener {
            val fecha = tv_fecha.text.toString()
            val paridad = sp_divisa.selectedItem.toString()
            val buySell = sp_buySell.selectedItem.toString()
            val inversion = et_inversion.text.toString()
            val rentabilidad = et_rentabilidad.text.toString()
            var resultado = sp_resultado.selectedItem.toString()

            if (inversion.isEmpty()){ //Inversion vacia
                et_inversion.setError("Ingrese su inversion")
                et_inversion.requestFocus()
            } else if (rentabilidad.isEmpty()) { //Rentabilidad vacia
                et_rentabilidad.setError("Ingrese la rentabilidad")
                et_rentabilidad.requestFocus()
            } else if (rentabilidad.toInt() > 100){
                et_rentabilidad.setError("No puede ser mayor a 100%")
                et_rentabilidad.requestFocus()
            } else if (fecha == "MM/DD/AAAA") { //Fecha incorrecta
                tv_fecha.error = "Por favor ingrese la fecha"
                Toast.makeText(requireContext(), "Ingrese una fecha valida", Toast.LENGTH_SHORT).show()
            } else {

                val correo = consultarCorreo()

                val database = FirebaseDatabase.getInstance()
                val myRef = database.getReference("usuarios")

                val postListener = object : ValueEventListener {
                    override fun onCancelled(error: DatabaseError) {
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        for (datasnapshot: DataSnapshot in dataSnapshot.children) {

                            val usuario = datasnapshot.getValue(UsuarioRemote::class.java)

                            if (usuario?.correo == correo) {
                                id1 = "${usuario?.id}"

                                guardarEnBitacora(database,resultado,inversion,rentabilidad,fecha,paridad,buySell)

                                registroOk()
                            }
                        }
                    }
                }
                myRef.addValueEventListener(postListener)
            }
        }
    }

    private fun guardarEnBitacora(
        database: FirebaseDatabase,
        resultado: String,
        inversion: String,
        rentabilidad: String,
        fecha: String,
        paridad: String,
        buySell: String
    ) {
        val myRef2: DatabaseReference = database.getReference("bitacora").child("$id1")

        val postListener2 = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                    maxId = (dataSnapshot.childrenCount.toInt())
            }
        }

        myRef2.addValueEventListener(postListener2)

        var ganancia: String = ""

        ganancia = calcularGanancia(resultado, inversion, rentabilidad, ganancia)

        val postListener3 = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                capital_user = dataSnapshot.getValue() as String
                //capital_user = ((capital_user.toFloat()) + (ganancia.toFloat())).toString()
                val valor = ((capital_user.toFloat()) + (ganancia.toFloat()))
                capital_user = (Math.round(valor * 10.0) / 10.0).toString()

                val bitacora = BitacoraRemote(
                    //(maxId+1).toString(),
                    maxId.toString(),
                    capital_user,
                    fecha,
                    paridad,
                    buySell,
                    inversion,
                    rentabilidad,
                    resultado,
                    ganancia
                )

                myRef2.child((maxId+1).toString()).setValue(bitacora)
            }
        }

        if (maxId <= 1) {
            myRef2.child("0").child("capitalInicial").addValueEventListener(postListener3)
        }
        else {
            myRef2.child("$maxId").child("capitalInicial").addValueEventListener(postListener3)
        }

    }

    private fun consultarCorreo(): String? {
        val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
        val user: FirebaseUser? = mAuth.currentUser
        val correo = user?.email
        return correo
    }

    private fun registroOk() {
        Toast.makeText(requireContext(), "Registro almacenado correctamente", Toast.LENGTH_SHORT).show()

        //Limpiar campos
        tv_fecha.setText("DD/MM/YYYY")
        et_rentabilidad.setText("")
        et_inversion.setText("")

        //Quitar Errores
        tv_fecha.setError(null)
        et_rentabilidad.setError(null)
        et_rentabilidad.setError(null)
    }

    private fun calcularGanancia(resultado: String, inversion: String, rentabilidad: String, ganancia: String): String {
        var ganancia1 = ganancia
        if (resultado == "Ganada") {
            val a = inversion.toFloat()
            val b = rentabilidad.toFloat() / 100
            //ganancia1 = ((a * b).toString())
            val valor = (a * b)
            ganancia1 = (Math.round(valor * 10.0) / 10.0).toString()
        } else {
            val valor = (-1 * inversion.toInt())
            ganancia1 = (Math.round(valor * 10.0) / 10.0).toString()
           // ganancia1 = (-1 * inversion.toInt()).toString()
        }
        return ganancia1
    }

    private fun rula() {
        val correo = consultarCorreo()

        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("usuarios")

        val postListener = object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (datasnapshot: DataSnapshot in dataSnapshot.children) {

                    val usuario = datasnapshot.getValue(UsuarioRemote::class.java)

                    if (usuario?.correo == correo) {
                        id1 = "${usuario?.id}"

                        val myRef2: DatabaseReference =  database.getReference("bitacora").child("$id1")
                        val postListener2 = object : ValueEventListener {
                            override fun onCancelled(error: DatabaseError) {
                            }

                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                maxId = (dataSnapshot.childrenCount.toInt())

                                val postListener3 = object : ValueEventListener {
                                    override fun onCancelled(error: DatabaseError) {}

                                    @SuppressLint("WrongConstant")
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        capital_user = dataSnapshot.getValue() as String

                                        if (capital_user == "0"){
                                            bt_guardar.setVisibility(View.GONE)
                                            bt_capital.setVisibility(View.VISIBLE)
                                            Toast.makeText(requireContext(),"Ingrese el capital inicial", Toast.LENGTH_LONG).show()
                                        } else{
                                           // bt_guardar.setVisibility(View.VISIBLE)
                                            bt_capital.setVisibility(View.GONE)
                                        }
                                    }
                                }

                                myRef2.child("0").child("capitalInicial").addValueEventListener(postListener3)

                            }
                        }

                        myRef2.addValueEventListener(postListener2)
                    }
                }
            }
        }

        myRef.addValueEventListener(postListener)
    }

    private fun cuadroDialogo() {
        var ModelDialog = AlertDialog.Builder(requireContext())
        val DialogVista = layoutInflater.inflate(R.layout.cuadro_dialogo, null)
        val botonCancelar = DialogVista.findViewById<Button>(R.id.bt_cancelar)
        val botonAceptar = DialogVista.findViewById<Button>(R.id.bt_aceptar)
        val capital = DialogVista.findViewById<EditText>(R.id.et_capital)
        ModelDialog.setView(DialogVista)
        var DialogoPersonalizado = ModelDialog.create()
        DialogoPersonalizado.show()

        botonCancelar.setOnClickListener {
            DialogoPersonalizado.dismiss()
        }



        botonAceptar.setOnClickListener {
            val correo = consultarCorreo()

            val database = FirebaseDatabase.getInstance()
            val myRef = database.getReference("usuarios")

            val postListener = object : ValueEventListener {
                override fun onCancelled(error: DatabaseError) {}

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (datasnapshot: DataSnapshot in dataSnapshot.children) {

                        val usuario = datasnapshot.getValue(UsuarioRemote::class.java)

                        if (usuario?.correo == correo) {
                            id1 = "${usuario?.id}"

                            if (capital.text.toString() == ""){
                                capital.setError("Ingrese su capital inicial")
                                capital.requestFocus()
                                Toast.makeText(requireContext(), "Ingrese el capital inicial", Toast.LENGTH_SHORT).show()
                            } else {
                                val myRef2: DatabaseReference =  database.getReference("bitacora").child("$id1")
                                myRef2.child("0").child("capitalInicial").setValue("${capital.text}")
                                Toast.makeText(requireContext(), "Guardado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            myRef.addValueEventListener(postListener)

            val ft: FragmentTransaction = requireFragmentManager().beginTransaction()
            ft.detach(this).attach(this).commit()
            DialogoPersonalizado.dismiss()
        }
    }
}