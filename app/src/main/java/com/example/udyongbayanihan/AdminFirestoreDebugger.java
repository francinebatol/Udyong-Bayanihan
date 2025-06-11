package com.example.udyongbayanihan;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class AdminFirestoreDebugger {
    private static final String TAG = "AdminFirestoreDebug";

    public static void debugAdminAccount(Context context, String adminId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        StringBuilder debugInfo = new StringBuilder();

        debugInfo.append("Starting debug for admin ID: ").append(adminId).append("\n");

        // Check AdminMobileAccount
        db.collection("AdminMobileAccount").document(adminId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        debugInfo.append("AdminMobileAccount found: ").append(documentSnapshot.getId()).append("\n");
                        debugInfo.append("  Username: ").append(documentSnapshot.getString("amUsername")).append("\n");
                        debugInfo.append("  Email: ").append(documentSnapshot.getString("amEmail")).append("\n");
                    } else {
                        debugInfo.append("AdminMobileAccount NOT found for ID: ").append(adminId).append("\n");

                        // If document not found, check if admin ID exists as a field in any document
                        db.collection("AdminMobileAccount").get()
                                .addOnSuccessListener(adminDocs -> {
                                    debugInfo.append("Searching all AdminMobileAccount documents...\n");
                                    for (DocumentSnapshot doc : adminDocs) {
                                        debugInfo.append("  Found document with ID: ").append(doc.getId()).append("\n");
                                    }
                                });
                    }

                    // Check AMOtherDetails - by field reference, not document ID
                    checkAMOtherDetails(context, adminId, debugInfo, db);
                })
                .addOnFailureListener(e -> {
                    debugInfo.append("Error accessing AdminMobileAccount: ").append(e.getMessage()).append("\n");
                    Log.e(TAG, "Error accessing AdminMobileAccount", e);

                    // Continue with other checks even if this fails
                    checkAMOtherDetails(context, adminId, debugInfo, db);
                });
    }

    private static void checkAMOtherDetails(Context context, String adminId, StringBuilder debugInfo, FirebaseFirestore db) {
        debugInfo.append("\nChecking AMOtherDetails where amAccountid field = ").append(adminId).append("\n");

        db.collection("AMOtherDetails").whereEqualTo("amAccountid", adminId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        debugInfo.append("AMOtherDetails found: ").append(doc.getId()).append("\n");
                        debugInfo.append("  Fields in document:\n");

                        for (String field : doc.getData().keySet()) {
                            debugInfo.append("    ").append(field).append(" = ").append(doc.get(field)).append("\n");
                        }
                    } else {
                        debugInfo.append("AMOtherDetails with amAccountid = ").append(adminId).append(" NOT found\n");

                        // Try alternate field name
                        db.collection("AMOtherDetails").whereEqualTo("amAccountId", adminId).get()
                                .addOnSuccessListener(altQuery -> {
                                    if (!altQuery.isEmpty()) {
                                        DocumentSnapshot doc = altQuery.getDocuments().get(0);
                                        debugInfo.append("AMOtherDetails found with alternate field name 'amAccountId': ")
                                                .append(doc.getId()).append("\n");

                                        for (String field : doc.getData().keySet()) {
                                            debugInfo.append("    ").append(field).append(" = ").append(doc.get(field)).append("\n");
                                        }
                                    } else {
                                        // If still not found, check all documents in collection
                                        db.collection("AMOtherDetails").get()
                                                .addOnSuccessListener(allDocs -> {
                                                    debugInfo.append("Listing ALL AMOtherDetails documents (").append(allDocs.size()).append(" found):\n");
                                                    for (QueryDocumentSnapshot document : allDocs) {
                                                        debugInfo.append("  Document ID: ").append(document.getId()).append("\n");

                                                        // Check if any field contains the admin ID
                                                        boolean foundId = false;
                                                        for (String field : document.getData().keySet()) {
                                                            Object value = document.get(field);
                                                            debugInfo.append("    ").append(field).append(" = ").append(value).append("\n");

                                                            if (value != null && value.toString().equals(adminId)) {
                                                                debugInfo.append("    *** FOUND ADMIN ID in field: ").append(field).append(" ***\n");
                                                                foundId = true;
                                                            }
                                                        }

                                                        if (foundId) {
                                                            debugInfo.append("  *** THIS DOCUMENT CONTAINS THE ADMIN ID ***\n");
                                                        }
                                                        debugInfo.append("\n");
                                                    }

                                                    // Show final debug info
                                                    displayDebugInfo(context, debugInfo.toString());
                                                });
                                    }
                                });
                    }

                    // Check AMNameDetails
                    checkAMNameDetails(context, adminId, debugInfo, db);
                })
                .addOnFailureListener(e -> {
                    debugInfo.append("Error accessing AMOtherDetails: ").append(e.getMessage()).append("\n");
                    Log.e(TAG, "Error accessing AMOtherDetails", e);

                    // Continue with other checks
                    checkAMNameDetails(context, adminId, debugInfo, db);
                });
    }

    private static void checkAMNameDetails(Context context, String adminId, StringBuilder debugInfo, FirebaseFirestore db) {
        debugInfo.append("\nChecking AMNameDetails where amAccountid field = ").append(adminId).append("\n");

        db.collection("AMNameDetails").whereEqualTo("amAccountid", adminId).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        debugInfo.append("AMNameDetails found: ").append(doc.getId()).append("\n");
                        debugInfo.append("  Fields in document:\n");

                        for (String field : doc.getData().keySet()) {
                            debugInfo.append("    ").append(field).append(" = ").append(doc.get(field)).append("\n");
                        }
                    } else {
                        debugInfo.append("AMNameDetails with amAccountid = ").append(adminId).append(" NOT found\n");

                        // Check alternate field name
                        db.collection("AMNameDetails").whereEqualTo("amAccountId", adminId).get()
                                .addOnSuccessListener(altQuery -> {
                                    if (!altQuery.isEmpty()) {
                                        DocumentSnapshot doc = altQuery.getDocuments().get(0);
                                        debugInfo.append("AMNameDetails found with alternate field name 'amAccountId': ")
                                                .append(doc.getId()).append("\n");
                                    } else {
                                        debugInfo.append("AMNameDetails not found with either field name\n");
                                    }

                                    // Display the final debug info
                                    displayDebugInfo(context, debugInfo.toString());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    debugInfo.append("Error accessing AMNameDetails: ").append(e.getMessage()).append("\n");
                    Log.e(TAG, "Error accessing AMNameDetails", e);

                    // Display debug info so far
                    displayDebugInfo(context, debugInfo.toString());
                });
    }

    private static void displayDebugInfo(Context context, String debugInfo) {
        Log.d(TAG, "DEBUG INFO:\n" + debugInfo);
    }
}