package com.cookigram.controller;

import com.cookigram.dto.OrderFormDto;
import com.cookigram.dto.PaymentFormDto;
import com.cookigram.model.Order;
import com.cookigram.model.User;
import com.cookigram.repository.UserRepository;
import com.cookigram.service.OrderService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/customer/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ─── Step 1: Order Form ───────────────────────────────────────────

    @GetMapping("/new")
    public String showOrderForm(Model model, HttpSession session) {
        OrderFormDto form = (OrderFormDto) session.getAttribute("orderForm");
        if (form == null) form = new OrderFormDto();
        model.addAttribute("orderForm", form);
        model.addAttribute("minDate", LocalDate.now().plusDays(4));
        model.addAttribute("maxDate", LocalDate.now().plusYears(1));
        model.addAttribute("provinces", getProvinces());
        return "customer/order-form";
    }

    @PostMapping("/new")
    public String processOrderForm(@Valid @ModelAttribute("orderForm") OrderFormDto form,
                                   BindingResult result,
                                   Model model,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        OrderService.ValidationResult validation = orderService.validateOrderForm(form);
        if (!validation.isValid()) {
            for (String error : validation.getErrors())
                result.rejectValue("deliveryDate", "error.orderForm", error);
        }

        if (result.hasErrors()) {
            model.addAttribute("minDate", LocalDate.now().plusDays(4));
            model.addAttribute("maxDate", LocalDate.now().plusYears(1));
            model.addAttribute("provinces", getProvinces());
            return "customer/order-form";
        }

        session.setAttribute("orderForm", form);
        return "redirect:/customer/order/review";
    }

    // ─── Step 2: Review Order ─────────────────────────────────────────

    @GetMapping("/review")
    public String showReviewPage(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        OrderFormDto form = (OrderFormDto) session.getAttribute("orderForm");
        if (form == null) {
            redirectAttributes.addFlashAttribute("error", "Please fill out the order form first");
            return "redirect:/customer/order/new";
        }
        model.addAttribute("orderForm", form);
        model.addAttribute("price", "24.99");
        return "customer/order-review";
    }

    @PostMapping("/review/edit")
    public String editOrder(HttpSession session) {
        return "redirect:/customer/order/new";
    }

    // ─── Step 3: Payment Selection ────────────────────────────────────

    @GetMapping("/payment")
    public String showPaymentSelection(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
        OrderFormDto form = (OrderFormDto) session.getAttribute("orderForm");
        if (form == null) {
            redirectAttributes.addFlashAttribute("error", "Please complete your order first");
            return "redirect:/customer/order/new";
        }
        return "customer/payment-selection";
    }

    // ─── Step 4: Payment Form ─────────────────────────────────────────

    @GetMapping("/payment/form")
    public String showPaymentForm(@RequestParam(required = false) String provider,
                                  Model model,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        OrderFormDto orderForm = (OrderFormDto) session.getAttribute("orderForm");
        if (orderForm == null) {
            redirectAttributes.addFlashAttribute("error", "Please complete your order first");
            return "redirect:/customer/order/new";
        }
        model.addAttribute("paymentForm", new PaymentFormDto());
        model.addAttribute("orderForm", orderForm);
        model.addAttribute("provider", provider != null ? provider : "card");
        model.addAttribute("price", "24.99");
        return "customer/payment-form";
    }

    @PostMapping("/payment/process")
    public String processPayment(@Valid @ModelAttribute("paymentForm") PaymentFormDto paymentForm,
                                 BindingResult result,
                                 Model model,
                                 HttpSession session,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {

        OrderFormDto orderForm = (OrderFormDto) session.getAttribute("orderForm");
        if (orderForm == null) {
            redirectAttributes.addFlashAttribute("error", "Session expired. Please start over.");
            return "redirect:/customer/order/new";
        }

        if (result.hasErrors()) {
            model.addAttribute("orderForm", orderForm);
            model.addAttribute("provider", "card");
            model.addAttribute("price", "24.99");
            return "customer/payment-form";
        }

        String transactionId = UUID.randomUUID().toString();

        // Double-click / duplicate submission prevention
        String sessionTransactionId = (String) session.getAttribute("lastTransactionId");
        if (transactionId.equals(sessionTransactionId)) {
            redirectAttributes.addFlashAttribute("error", "Order already submitted");
            return "redirect:/customer/dashboard";
        }

        try {
            User customer = getCurrentUser(auth);
            Order order = orderService.createOrder(customer, orderForm, transactionId);

            session.removeAttribute("orderForm");
            session.setAttribute("lastTransactionId", transactionId);
            session.setAttribute("lastOrderId", order.getId());

            return "redirect:/customer/order/confirmation/" + order.getId();

        } catch (OrderService.DuplicateOrderException e) {
            redirectAttributes.addFlashAttribute("error", "Order already submitted");
            return "redirect:/customer/dashboard";

        } catch (OrderService.CapacityExceededException e) {
            redirectAttributes.addFlashAttribute("error", "Selected date is fully booked. Please choose another date.");
            return "redirect:/customer/order/new";

        } catch (Exception e) {
            model.addAttribute("error", "An error occurred processing your payment. Please try again.");
            model.addAttribute("orderForm", orderForm);
            model.addAttribute("provider", "card");
            model.addAttribute("price", "24.99");
            return "customer/payment-form";
        }
    }

    // ─── Step 5: Order Confirmation ───────────────────────────────────

    @GetMapping("/confirmation/{orderId}")
    public String showConfirmation(@PathVariable String orderId,
                                   Model model,
                                   Authentication auth,
                                   RedirectAttributes redirectAttributes) {
        User customer = getCurrentUser(auth);
        Order order = orderService.getOrderByIdAndCustomer(orderId, customer).orElse(null);

        if (order == null) {
            redirectAttributes.addFlashAttribute("error", "Order not found");
            return "redirect:/customer/dashboard";
        }

        model.addAttribute("order", order);
        return "customer/order-confirmation";
    }

    // ─── Cancel Order ─────────────────────────────────────────────────

    @PostMapping("/cancel/{orderId}")
    public String cancelOrder(@PathVariable String orderId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            User customer = getCurrentUser(auth);
            orderService.cancelOrder(orderId, customer);
            redirectAttributes.addFlashAttribute("success",
                "Your order has been cancelled. Any payment refund will be processed within 3 business days.");

        } catch (OrderService.OrderNotFoundException e) {
            redirectAttributes.addFlashAttribute("error", "Order not found");

        } catch (OrderService.OrderCancellationException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/customer/dashboard";
    }

    // ─── Helper ───────────────────────────────────────────────────────

    private String[] getProvinces() {
        return new String[]{
            "AB - Alberta",
            "BC - British Columbia",
            "MB - Manitoba",
            "NB - New Brunswick",
            "NL - Newfoundland and Labrador",
            "NS - Nova Scotia",
            "NT - Northwest Territories",
            "NU - Nunavut",
            "ON - Ontario",
            "PE - Prince Edward Island",
            "QC - Quebec",
            "SK - Saskatchewan",
            "YT - Yukon"
        };
    }
}