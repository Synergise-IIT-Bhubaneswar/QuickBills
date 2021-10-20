package com.sushant.quickbills.activity

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.sushant.quickbills.R
import com.sushant.quickbills.data.USERS_FIELD
import com.sushant.quickbills.model.User
import kotlinx.android.synthetic.main.activity_sign_up.*


class SignUpActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        //Initialising variables
        auth = Firebase.auth
        database = Firebase.database.reference

        //Setting up all the listeners
        sign_up_in_proceed_btn.setOnClickListener {
            signUpUser()
        }
    }

    private fun signUpUser() {
        val userName: String = user_name_entered_id.text.toString()
        val userEmail: String = sign_up_email_entered_id.text.toString()
        val userPassword: String = sign_up_password_entered_id.text.toString()
        val userPasswordConfirm: String = sign_up_confirm_password_entered_id.text.toString()

        //If the entered name is empty
        if (userName.trim().isEmpty()) {
            user_name_entered_id.error = "Please enter your name!!"
            user_name_entered_id.requestFocus()
            return
        }

        //If the email entered is not valid
        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            sign_up_email_entered_id.error = "Please enter a valid email!!"
            sign_up_email_entered_id.requestFocus()
            return
        }

        //If the password doesn't match constraints
        if (userPassword.length < 8) {
            sign_up_password_entered_id.error = "The password length must be atleast 8!!"
            sign_up_password_entered_id.requestFocus()
            return
        }

        //If the passwords do not match
        if (!userPassword.contentEquals(userPasswordConfirm)) {
            sign_up_confirm_password_entered_id.error = "Both the passwords do not match!!"
            sign_up_confirm_password_entered_id.requestFocus()
            return
        }

        //Disable Multiple SignUp requests before async tasks:
        sign_up_in_proceed_btn.isEnabled = false
        sign_up_in_proceed_btn.isClickable = false

        //Creating now actual user
        auth.createUserWithEmailAndPassword(
            userEmail, userPassword
        ).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                //Set user profile
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(userName).build()
                val newUser = User()

                //These activities need to be done simultaneously
                val profileUpdateTask = user!!.updateProfile(profileUpdates)
                val addUserToDbTask = database.child(USERS_FIELD).child(auth.currentUser!!.uid).setValue(newUser)

                val combinedTask = Tasks.whenAll(mutableListOf(profileUpdateTask, addUserToDbTask))
                combinedTask.addOnCompleteListener {
                    combined_task_result->
                    if(combined_task_result.isSuccessful){
                        user.sendEmailVerification().addOnCompleteListener{
                            send_mail_task->
                            if(send_mail_task.isSuccessful){
                                Toast.makeText(this, "User Registered Successfully", Toast.LENGTH_SHORT).show()
                                Toast.makeText(this, "Check your inbox and verify your email!!", Toast.LENGTH_SHORT).show()
                                finish()
                            }else{
                                sign_up_in_proceed_btn.isEnabled = true
                                sign_up_in_proceed_btn.isClickable = true
                                Log.w("Error", send_mail_task.exception)
                                Toast.makeText(this, send_mail_task.exception!!.localizedMessage, Toast.LENGTH_LONG).show()
                            }
                        }
                    }else{  //Revert the changes now
                        if(addUserToDbTask.isSuccessful)
                            database.child(USERS_FIELD).child(auth.currentUser!!.uid).removeValue()
                        if(auth.currentUser != null)
                            auth.currentUser!!.delete()
                        sign_up_in_proceed_btn.isEnabled = true
                        sign_up_in_proceed_btn.isClickable = true
                        Log.e("Error", combined_task_result.exception.toString())
                        Toast.makeText(this, combined_task_result.exception!!.localizedMessage, Toast.LENGTH_LONG).show()
                    }
                }

            } else {
                sign_up_in_proceed_btn.isEnabled = true
                sign_up_in_proceed_btn.isClickable = true
                Log.w("Error", "SignUpWithEmailCreate:Failure", task.exception)
                Toast.makeText(this, task.exception!!.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
}