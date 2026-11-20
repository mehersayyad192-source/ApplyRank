package com.applyrank;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Resume Polish with an uploaded PDF or DOCX file instead of pasted text.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"spring.datasource.url=jdbc:h2:mem:polishfiledb;DB_CLOSE_DELAY=-1", "app.upload-dir=./target/test-uploads", "app.ai.api-key="})
class ResumePolishFileApiTest {
    @Autowired MockMvc mvc;

    private String login(String email) throws Exception {
        String res = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"secret12\",\"name\":\"Lab\"}")).andReturn().getResponse().getContentAsString();
        return "Bearer " + res.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    // a tiny real PDF made in memory, with one line of text
    private byte[] pdf(String line) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(line);
                cs.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }

    // a tiny DOCX: it is a zip file with word/document.xml inside
    private byte[] docx(String line) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry("word/document.xml"));
            z.write(("<w:document><w:body><w:p><w:r><w:t>" + line + "</w:t></w:r></w:p></w:body></w:document>").getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return out.toByteArray();
    }

    @Test
    void needsLogin() throws Exception {
        mvc.perform(multipart("/api/ailab/resume-polish/file").file(new MockMultipartFile("file", "r.pdf", "application/pdf", new byte[]{1, 2, 3})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pdfIsReadAndPrivateDetailsAreHidden() throws Exception {
        String t = login("polishfile1@x.dev");
        byte[] data = pdf("Skills Java Spring Boot. Contact student@example.com for projects");
        String out = mvc.perform(multipart("/api/ailab/resume-polish/file").file(new MockMultipartFile("file", "r.pdf", "application/pdf", data))
                        .param("useAi", "false").header("Authorization", t))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(out.contains("event: rules"), out);
        assertTrue(out.contains("[email]"), out);
        assertFalse(out.contains("student@example.com"), "the email must never come back");
    }

    @Test
    void docxIsReadToo() throws Exception {
        String t = login("polishfile2@x.dev");
        String out = mvc.perform(multipart("/api/ailab/resume-polish/file")
                        .file(new MockMultipartFile("file", "r.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                docx("Education B.Tech 2021-2025 and Skills Java and Projects")))
                        .header("Authorization", t))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(out.contains("event: rules"), out);
    }

    @Test
    void otherFilesAndEmptyFilesAreRejected() throws Exception {
        String t = login("polishfile3@x.dev");
        mvc.perform(multipart("/api/ailab/resume-polish/file")
                        .file(new MockMultipartFile("file", "r.txt", "text/plain", "just some plain text that is long enough".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", t))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/ailab/resume-polish/file")
                        .file(new MockMultipartFile("file", "r.pdf", "application/pdf", new byte[0]))
                        .header("Authorization", t))
                .andExpect(status().isBadRequest());
    }
}
