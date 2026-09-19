package com.hospital.app.controller.lab;

import com.hospital.app.dto.lab.LabOrderStatusRequest;
import com.hospital.app.dto.lab.LabOrderItemResultRequest;
import com.hospital.app.dto.lab.LabReportRequest;
import com.hospital.app.model.lab.LabOrder;
import com.hospital.app.model.lab.LabOrderItem;
import com.hospital.app.model.lab.LabReport;
import com.hospital.app.model.lab.LabTestMaster;
import com.hospital.app.service.lab.LabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lab")
public class LabController {
    @Autowired
    private LabService labService;

    @GetMapping("/tests")
    public ResponseEntity<List<LabTestMaster>> getAllTests() {
        return ResponseEntity.ok(labService.getAllTests());
    }

    @PostMapping("/tests")
    public ResponseEntity<LabTestMaster> addTest(@jakarta.validation.Valid @RequestBody LabTestMaster test) {
        return ResponseEntity.ok(labService.addTest(test));
    }

    @GetMapping("/orders")
    public ResponseEntity<List<LabOrder>> getOrders(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(labService.getOrdersByStatus(status));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<Map<String, Object>> getOrderDetails(@PathVariable Long id) {
        LabOrder order = labService.getOrderById(id);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        List<LabOrderItem> items = labService.getOrderItems(id);
        Map<String, Object> response = new HashMap<>();
        response.put("order", order);
        response.put("items", items);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{id}/items")
    public ResponseEntity<List<LabOrderItem>> getOrderItems(@PathVariable Long id) {
        return ResponseEntity.ok(labService.getOrderItems(id));
    }

    @PutMapping("/orders/{id}/status")
    public ResponseEntity<LabOrder> updateOrderStatus(@PathVariable Long id, @jakarta.validation.Valid @RequestBody LabOrderStatusRequest request) {
        return ResponseEntity.ok(labService.updateOrderStatus(id, request.getStatus()));
    }

    @PostMapping("/reports")
    public ResponseEntity<LabReport> addReport(@jakarta.validation.Valid @RequestBody LabReportRequest request,
                                               jakarta.servlet.http.HttpServletRequest httpRequest) {
        LabReport report = new LabReport();
        report.setVisitId(request.getVisitId());
        report.setLabOrderId(request.getLabOrderId());
        report.setFileName(request.getFileName());
        report.setFileUrl(request.getFileUrl());
        report.setMimeType(request.getMimeType());
        var actor = (com.hospital.app.dto.LoginResponse) httpRequest.getSession()
            .getAttribute(com.hospital.app.controller.LoginController.AUTHENTICATED_USER);
        String performedBy = actor == null ? "Lab team" : actor.getUsername() + " (" + actor.getRole() + ")";
        return ResponseEntity.ok(labService.addReport(report, performedBy));
    }

    @PutMapping("/order-items/{id}")
    public ResponseEntity<LabOrderItem> updateOrderItem(@PathVariable Long id, @jakarta.validation.Valid @RequestBody LabOrderItemResultRequest request) {
        return ResponseEntity.ok(labService.updateOrderItemResult(id, request));
    }

    @GetMapping("/reports/{id}/pdf")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Long id) {
        byte[] pdfBytes = labService.generateLabReportPdf(id);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=lab-report-" + id + ".pdf")
                .body(pdfBytes);
    }
}
