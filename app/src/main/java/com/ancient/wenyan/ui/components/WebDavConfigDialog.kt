package com.ancient.wenyan.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ancient.wenyan.domain.sync.WebDavConfig
import com.ancient.wenyan.ui.theme.StudyBlueAccent

@Composable
fun WebDavConfigDialog(
    initialConfig: WebDavConfig = WebDavConfig(),
    isSyncing: Boolean = false,
    onDismiss: () -> Unit,
    onUpload: (WebDavConfig) -> Unit,
    onDownload: (WebDavConfig) -> Unit
) {
    var serverUrl by remember { mutableStateOf(initialConfig.serverUrl.ifBlank { "https://dav.jianguoyun.com/dav/" }) }
    var username by remember { mutableStateOf(initialConfig.username) }
    var password by remember { mutableStateOf(initialConfig.password) }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = StudyBlueAccent,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "WebDAV 云端同步与备份",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "支持坚果云、Infuse、Nextcloud 等标准 WebDAV 服务，将在根目录存取 wenyan_backup.json 进行全量数据同步。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (isSyncing) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = StudyBlueAccent
                    )
                }

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text("WebDAV 服务器地址") },
                    placeholder = { Text("https://dav.jianguoyun.com/dav/") },
                    singleLine = true,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("账号 / 邮箱") },
                    singleLine = true,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("应用授权密码") },
                    singleLine = true,
                    enabled = !isSyncing,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "隐藏密码" else "显示密码"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val config = WebDavConfig(serverUrl.trim(), username.trim(), password.trim())
                        onDownload(config)
                    },
                    enabled = !isSyncing && serverUrl.isNotBlank(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("云端恢复", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val config = WebDavConfig(serverUrl.trim(), username.trim(), password.trim())
                        onUpload(config)
                    },
                    enabled = !isSyncing && serverUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = StudyBlueAccent),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("上传备份", fontSize = 12.sp, color = Color.White)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSyncing
            ) {
                Text("关闭")
            }
        }
    )
}
