package pl.coderslab.final_project.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import pl.coderslab.final_project.domain.Cart;
import pl.coderslab.final_project.domain.CartItem;
import pl.coderslab.final_project.domain.Product;
import pl.coderslab.final_project.domain.User;
import pl.coderslab.final_project.service.CartItemRepository;
import pl.coderslab.final_project.service.CartRepository;
import pl.coderslab.final_project.service.ProductRepository;
import pl.coderslab.final_project.service.UserRepository;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
@SessionAttributes("cart")
public class CartController {
    CartRepository cartRepository;
    ProductRepository productRepository;
    CartItemRepository cartItemRepository;
    UserRepository userRepository;

    public CartController(CartRepository cartRepository, ProductRepository productRepository,
                          CartItemRepository cartItemRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }

    @RequestMapping("/addToCart/{productId}")
    @ResponseBody
    public String addToCart(@Valid CartItem cartItem, BindingResult result,
                            @PathVariable("productId") Long productId, Model model, HttpSession session) {
        Product product = productRepository.findById(productId).orElseThrow(IllegalArgumentException::new);
        if (result.hasErrors()) {
            model.addAttribute("product", product);
            return "product/productDetails";
        }
        Cart cart = new Cart();

        if (session.getAttribute("cart") == null &&
                session.getAttribute("loggedUser") == null) {
            cart = new Cart();

        } else if (session.getAttribute("cart") == null &&
                session.getAttribute("loggedUser") != null) {
            User user = (User) session.getAttribute("loggedUser");
            if (user.getCart() == null) {
                cart = new Cart();
                cart = cartRepository.save(cart);
                user.setCart(cart);
                userRepository.save(user);
                model.addAttribute("loggedUser", user);
            } else {
                cart = user.getCart();
            }

        } else if (session.getAttribute("cart") != null &&
                session.getAttribute("loggedUser") == null) {
            cart = (Cart) session.getAttribute("cart");

        } else if (session.getAttribute("cart") != null &&
                session.getAttribute("loggedUser") != null) {
            User user = (User) session.getAttribute("loggedUser");
            cart = (Cart) session.getAttribute("cart");

            if (user.getCart() != cart) {
//                Cart oldCart = user.getCart();
//                cartItemRepository.updateAllCartItemsOldCartToNewCart(cart, oldCart);
//
//                user.setCart(cart);
//                userRepository.save(user);
//                cartRepository.delete(oldCart);
                Cart newCart = user.getCart();
                cartItemRepository.updateAllCartItemsOldCartToNewCart(newCart, cart);

                cartRepository.delete(cart);
                cart = newCart;
            }

            else if (user.getCart() == null) {
                user.setCart(cart);
                user = userRepository.save(user);

            } else {
                Cart newCart = user.getCart();
                cartItemRepository.updateAllCartItemsOldCartToNewCart(newCart, cart);

                cartRepository.delete(cart);
                cart = newCart;
            }
            model.addAttribute("loggedUser", user);
        }

        cart.setItemsQuantity(cart.getItemsQuantity() + cartItem.getQuantity());
        cartRepository.save(cart);
        CartItem existingCartItem = cartItemRepository.findFirstByProductAndCart(product, cart).orElse(null);
        boolean isProductInCart = cartItemRepository.findFirstByProductAndCart(product, cart).isPresent();

        if (existingCartItem != null) {
            cartItem.setQuantity(existingCartItem.getQuantity() + cartItem.getQuantity());
        }

        cartItem.setCart(cart);
        cartItem.setProduct(product);

        cartRepository.save(cart);
        cartItemRepository.save(cartItem);
        model.addAttribute("cart", cart);

        String message = "";

        StringBuilder collect = new StringBuilder();
        int count = 0;
        List<String> collect1 = cartItemRepository.findAllByCart(cart).stream()
                .map(CartItem::toString)
                .collect(Collectors.toList());

        for (String s : collect1) {
            collect.append(++count).append(". ").append(s).append("<br>");
        }

        List<String> res = new ArrayList<>();
        res.add(cart.toString());
        res.add(cartItem.toString());
        res.add(product.toString());
        res.add(collect.toString());
        res.add("<a href=\"/\">Home</a>");
        res.add("Product in cart:" + isProductInCart);
        return res.stream().collect(Collectors.joining("<br>".repeat(2)));
    }

    @RequestMapping("/cartDetails")
    public String showCart(HttpSession session, Model model) {
        List<CartItem> cartItems;
        LinkedHashMap<CartItem, Product> map = new LinkedHashMap<>();
        if (session.getAttribute("cart") != null) {
            Cart cart = (Cart) session.getAttribute("cart");
            cartItems = cartItemRepository.findAllByCart(cart);
            cartItems.forEach(cartItem -> map.put(cartItem,
                    productRepository.findById(cartItem.getProduct().getId()).orElse(null)));
        }
        model.addAttribute("cartItems", map);
        return "cart/cartDetails";
    }

    @RequestMapping("/deleteFromCart")
//    @ResponseBody
    public void deleteFromCart(@RequestParam("id") Long id, Model model) {
        CartItem cartItem = cartItemRepository.findById(id).orElseThrow(IllegalArgumentException::new);
        Cart cart = cartRepository.findById(cartItem.getCart().getId())
                .orElseThrow(IllegalArgumentException::new);
        cart.setItemsQuantity(cart.getItemsQuantity() - cartItem.getQuantity());
        cartRepository.save(cart);
        model.addAttribute("cart", cart);
        cartItemRepository.deleteById(id);
//        return "redirect:cartDetails";
    }

}
