package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ContextNoteEntity
import com.example.data.local.TokenUsageEntity
import com.example.ui.components.TokenExpenditureVisualizer
import com.example.ui.theme.NeutralBackground
import com.example.ui.theme.NeutralOnSurface
import com.example.ui.theme.NeutralOnSurfaceVariant
import com.example.ui.theme.NeutralOutline
import com.example.ui.theme.NeutralSurface
import com.example.ui.theme.PurpleOnPrimary
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryContainer
import com.example.ui.viewmodel.MatterBillingSummary

@Composable
fun BillingTab(
  billingSummaries: List<MatterBillingSummary>,
  onAddMatter: (code: String, name: String, client: String) -> Unit,
  tokenMetrics: List<TokenUsageEntity> = emptyList(),
  notes: List<ContextNoteEntity> = emptyList(),
  modifier: Modifier = Modifier
) {
  var selectedSubTab by remember { mutableStateOf(0) }
  var codeText by remember { mutableStateOf("") }
  var nameText by remember { mutableStateOf("") }
  var clientText by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NeutralBackground)
  ) {
    // Segmented Sub-Tab Switcher
    TabRow(
      selectedTabIndex = selectedSubTab,
      containerColor = NeutralSurface,
      contentColor = PurplePrimary,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSubTab]),
          color = PurplePrimary
        )
      },
      modifier = Modifier.fillMaxWidth().testTag("billing_sub_tabs")
    ) {
      Tab(
        selected = selectedSubTab == 0,
        onClick = { selectedSubTab = 0 },
        text = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.BusinessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Matter Billing", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        },
        modifier = Modifier.testTag("matter_billing_subtab")
      )
      Tab(
        selected = selectedSubTab == 1,
        onClick = { selectedSubTab = 1 },
        text = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("AI Token Expenditure", fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        },
        modifier = Modifier.testTag("token_expenditure_subtab")
      )
    }

    if (selectedSubTab == 1) {
      TokenExpenditureVisualizer(
        tokenMetrics = tokenMetrics,
        notes = notes,
        modifier = Modifier.fillMaxSize()
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .background(NeutralBackground)
          .padding(horizontal = 16.dp)
          .testTag("billing_tab")
      ) {
        item {
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Billing & Matter Rollups",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = NeutralOnSurface
              )
              Text(
                text = "2-Hour block windows automatically calculated using epoch floor.",
                fontSize = 12.sp,
                color = NeutralOnSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Add Matter Card
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("add_matter_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = NeutralSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeutralOutline)
          ) {
            Column(modifier = Modifier.padding(16.dp)) {
              Text(
                text = "Create Matter Code",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary
              )

              Spacer(modifier = Modifier.height(10.dp))

              Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                  value = codeText,
                  onValueChange = { codeText = it },
                  placeholder = { Text("Code (e.g. LGL-101)", fontSize = 11.sp) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("matter_code_input"),
                  shape = RoundedCornerShape(10.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = NeutralOutline
                  )
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                  value = nameText,
                  onValueChange = { nameText = it },
                  placeholder = { Text("Matter Name", fontSize = 11.sp) },
                  modifier = Modifier
                    .weight(1.2f)
                    .testTag("matter_name_input"),
                  shape = RoundedCornerShape(10.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = NeutralOutline
                  )
                )
              }

              Spacer(modifier = Modifier.height(8.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = clientText,
                  onValueChange = { clientText = it },
                  placeholder = { Text("Client Name", fontSize = 11.sp) },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("matter_client_input"),
                  shape = RoundedCornerShape(10.dp),
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurplePrimary,
                    unfocusedBorderColor = NeutralOutline
                  )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                  onClick = {
                    onAddMatter(codeText.trim().uppercase(), nameText.trim(), clientText.trim())
                    codeText = ""
                    nameText = ""
                    clientText = ""
                  },
                  enabled = codeText.isNotBlank() && nameText.isNotBlank(),
                  colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("add_matter_button")
                ) {
                  Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = PurpleOnPrimary)
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Add", color = PurpleOnPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          Text(
            text = "Active Matters & Rollups",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = NeutralOnSurface
          )

          Spacer(modifier = Modifier.height(8.dp))
        }

        items(billingSummaries, key = { it.matterCode }) { summary ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp)
              .testTag("matter_summary_card_${summary.matterCode}"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NeutralSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeutralOutline)
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PurplePrimaryContainer),
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = summary.matterCode,
                    tint = PurplePrimary,
                    modifier = Modifier.size(20.dp)
                  )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = summary.matterCode,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                  )
                  Text(
                    text = "${summary.matterName} • ${summary.clientName}",
                    fontSize = 12.sp,
                    color = NeutralOnSurfaceVariant
                  )
                }
              }

              Column(horizontalAlignment = Alignment.End) {
                Text(
                  text = "${String.format(java.util.Locale.US, "%.1f", summary.loggedHours)} hrs",
                  fontSize = 16.sp,
                  fontWeight = FontWeight.ExtraBold,
                  color = NeutralOnSurface
                )
                Text(
                  text = "${summary.entriesCount} logs",
                  fontSize = 11.sp,
                  color = NeutralOnSurfaceVariant
                )
              }
            }
          }
        }

        item {
          Spacer(modifier = Modifier.height(80.dp))
        }
      }
    }
  }
}
