
package com.cvm.pdfeditor;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.widget.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;

public class MainActivity extends Activity {

    private static final int OPEN_PDF = 100;
    private static final int SAVE_PDF = 200;

    private Uri inputUri;
    private EditText nameInput, pageInput, xInput, yInput, sizeInput;
    private TextView status;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PDFBoxResourceLoader.init(getApplicationContext());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 20, 24, 20);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("CVM Pdf Editor");
        title.setTextSize(27);
        title.setTextColor(Color.BLUE);
        root.addView(title);

        Button open = new Button(this);
        open.setText("Open PDF");
        root.addView(open);

        nameInput = addInput(root, "Enter name or text");
        pageInput = addInput(root, "Page number (default 1)");
        xInput = addInput(root, "X position (default 50)");
        yInput = addInput(root, "Y position (default 100)");
        sizeInput = addInput(root, "Font size (default 18)");

        Button save = new Button(this);
        save.setText("Add Text & Save PDF");
        root.addView(save);
        saveButton = save;

        status = new TextView(this);
        status.setTextColor(Color.DKGRAY);
        root.addView(status);

        setContentView(root);

        open.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("application/pdf");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, OPEN_PDF);
        });

        save.setOnClickListener(v -> {
            if (inputUri == null) {
                status.setText("Please open a PDF first.");
                return;
            }
            if (nameInput.getText().toString().trim().isEmpty()) {
                status.setText("Enter a name first.");
                return;
            }

            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.setType("application/pdf");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.putExtra(Intent.EXTRA_TITLE, "CVM_Edited.pdf");
            startActivityForResult(i, SAVE_PDF);
        });
    }

    private EditText addInput(LinearLayout root, String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        root.addView(e);
        return e;
    }

    private float getNumber(EditText e, float fallback) {
        try {
            String s = e.getText().toString().trim();
            return s.isEmpty() ? fallback : Float.parseFloat(s);
        } catch (Exception ex) {
            return fallback;
        }
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null) return;

        if (request == OPEN_PDF) {
            inputUri = data.getData();
            status.setText("PDF selected.");
        } else if (request == SAVE_PDF) {
            Uri output = data.getData();
            if (output != null) editPdf(output);
        }
    }

    private void editPdf(Uri outputUri) {
        saveButton.setEnabled(false);
        try (InputStream in =
                 getContentResolver().openInputStream(inputUri)) {

            if (in == null) throw new IOException("Cannot open PDF");

            try (PDDocument doc = PDDocument.load(in)) {
                int pageNo = (int) getNumber(pageInput, 1);

                if (pageNo < 1 || pageNo > doc.getNumberOfPages()) {
                    throw new IOException("Invalid page number");
                }

                PDPage page = doc.getPage(pageNo - 1);
                float x = getNumber(xInput, 50);
                float y = getNumber(yInput, 100);
                float size = getNumber(sizeInput, 18);
                String name = nameInput.getText().toString().trim();

                if (size <= 0 || size > 200) {
                    throw new IOException("Invalid font size");
                }

                if (x < 0 || y < 0 ||
                    x > page.getMediaBox().getWidth() ||
                    y > page.getMediaBox().getHeight()) {
                    throw new IOException("X/Y outside page");
                }

                try (PDPageContentStream stream =
                    new PDPageContentStream(
                        doc, page,
                        PDPageContentStream.AppendMode.APPEND,
                        true, true)) {

                    stream.beginText();
                    stream.setFont(PDType1Font.HELVETICA, size);
                    stream.newLineAtOffset(x, y);
                    stream.showText(name);
                    stream.endText();
                }

                try (OutputStream out =
                    getContentResolver().openOutputStream(outputUri)) {
                    if (out == null) throw new IOException("Cannot save PDF");
                    doc.save(out);
                }
            }

            status.setText("PDF saved successfully!");

        } catch (Exception e) {
            status.setText("Error: " + e.getMessage());
        } finally {
            saveButton.setEnabled(true);
        }
    }
}
