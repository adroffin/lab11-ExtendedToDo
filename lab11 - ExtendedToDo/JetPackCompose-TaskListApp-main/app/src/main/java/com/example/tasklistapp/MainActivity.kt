package com.example.tasklistapp

import android.Manifest
import android.content.ClipData.Item
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.tasklistapp.ui.theme.TaskListAppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TaskListAppTheme {
                TaskListScreen()
            }
        }
    }
}

// Funcion en la que se crean los estados los cuales se enviaran a otras.
@Composable
fun TaskListScreen() {
    val context = LocalContext.current

    // State Hoisting: Elevamos el estado para gestionar las tareas y las imágenes
    val tasks = remember { mutableStateListOf<Task>() }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Solicitar permisos en el inicio
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    TaskListContent(
        tasks = tasks,
        onAddTask = { title, imageUri ->
            tasks.add(Task(title, imageUri))
        },
        onDeleteTask = {task ->
            tasks.remove(task)
        },
        onUpdateTask = { task, newTitle, newImageUri ->
            val index = tasks.indexOf(task)
            if (index != -1) {
                tasks[index] = task.copy(title = newTitle, imageUri = newImageUri)
            }
        },
    )
}

// Formulario para agregar la misma acompañada de una imagen.
// Tambien se maneja la logica del AlertDialog que aparece al querer editar una tarea.
@Composable
fun TaskListContent(
    tasks: List<Task>,
    onAddTask: (String, String?) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onUpdateTask: (Task, String, String? ) -> Unit
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<String?>(null) }
    var taskEdited by remember { mutableStateOf<Task?>(null) }
    var showEditingForm by remember { mutableStateOf(false) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedImageUri = uri.toString()
    }

    // El AlertDialog aparece si se esta editando una tarea.
    if(showEditingForm && taskEdited != null) {
        AlertDialog(
            onDismissRequest = { showEditingForm = false },
            title = { Text("Edit Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                            Text("Pick Image")
                        }
                        selectedImageUri?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = "Task Image",
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            },
            // Si hubieron cambios y estos se guardan, se actualiza la informacion y se limpian los estados al final.
            confirmButton = {
                Button(
                    onClick = {
                        if (newTaskTitle.isNotEmpty()) {
                            taskEdited?.let {
                                onUpdateTask(it, newTaskTitle, selectedImageUri)
                            }
                            showEditingForm = false
                            taskEdited = null
                            newTaskTitle = ""
                            selectedImageUri = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            // Si se cancela la operacion de edicion de alguna tarea, se fila el estado que muestra el AlertDialog para que no se muestre mas y se limpian los estados.
            dismissButton = {
                Button(onClick = {
                    showEditingForm = false
                    taskEdited = null
                    newTaskTitle = ""
                    selectedImageUri = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Task List", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = newTaskTitle,
            onValueChange = { newTaskTitle = it },
            label = { Text("Task Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Pick Image")
            }

            selectedImageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Selected Image",
                    modifier = Modifier.size(64.dp)
                )
            }

            Button(
                onClick = {
                    if (newTaskTitle.isNotEmpty()) {
                        onAddTask(newTaskTitle, selectedImageUri)
                        newTaskTitle = ""
                        selectedImageUri = null
                    }
                }
            ) {
                Text("Add Task")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TaskList(
            tasks = tasks,
            onDeleteTask = onDeleteTask,
            onEditTask = { task ->
                taskEdited = task
                newTaskTitle = task.title
                selectedImageUri = task.imageUri
                showEditingForm = true
            }
        )
    }
}

// Lista de tareas que actua como contenedor para cada uno de los items, desplegando estos mismos con una LazyColumn.
@Composable
fun TaskList(tasks: List<Task>, onDeleteTask: (Task) -> Unit, onEditTask: (Task) -> Unit) {
    LazyColumn {
        items(tasks) { task ->
            TaskItem(task, onDeleteTask, onEditTask)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// Tareas individuales que conforman la lista siendo desplegadas con su titulo, imagen y dos botones como parte del CRUD para editar o eliminar alguna tarea.
@Composable
fun TaskItem(task: Task, onDeleteTask: (Task) -> Unit, onEditTask: (Task) -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {

        task.imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.size(85.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))

        // Boton con el icono de Edit para manejar la funcion de editar una tarea
        IconButton(onClick = { onEditTask(task) }){
            Icon(Icons.Filled.Edit, contentDescription = "Edit Task")
        }
        // Boton con el icono de Delete para manejar la funcion de eliminar una tarea
        IconButton(onClick = { onDeleteTask(task) }){
            Icon(Icons.Filled.Delete, contentDescription = "Delete Task")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    TaskListAppTheme {
        TaskListScreen()
    }
}

data class Task(val title: String, val imageUri: String?)