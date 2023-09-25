package pl.coderslab.final_project.others;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CartControllerTest {
    CartRepository cartRepository;
    ProductRepository productRepository;
    CartItemRepository cartItemRepository;
    UserRepository userRepository;

    public CartControllerTest(CartRepository cartRepository, ProductRepository productRepository,
                              CartItemRepository cartItemRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.userRepository = userRepository;
    }


    //    @RequestMapping("/addToCart/{productId}")
//    @ResponseBody
    public String addToCartTest(@Valid CartItem cartItem, BindingResult result,
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
            cart = cartRepository.save(cart);
            model.addAttribute("cart", cart);
        } else if (session.getAttribute("cart") == null &&
                session.getAttribute("loggedUser") != null) {
            User user = (User) session.getAttribute("loggedUser");
            if (user.getCart() == null) {
                cart = new Cart();
                cart = cartRepository.save(cart);
                user.setCart(cart);
            } else {
                cart = user.getCart();
            }
            model.addAttribute("cart", cart);
            model.addAttribute("loggedUser", user);
        } else if (session.getAttribute("cart") != null &&
                session.getAttribute("loggedUser") == null) {
            cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                cart = new Cart();
            }
            cart = cartRepository.save(cart);
            model.addAttribute("cart", cart);
        } else if (session.getAttribute("cart") != null &&
                session.getAttribute("loggedUser") != null) {
            User user = (User) session.getAttribute("loggedUser");
            cart = (Cart) session.getAttribute("cart");
            if (cart == null) {
                if (user.getCart() == null) {
                    cart = new Cart();
                } else {
                    cart = user.getCart();
                }
            } else {
                if (user.getCart() == null) {
                    user.setCart(cart);
                } else {
                    cartItemRepository.updateAllCartItemsOldCartToNewCart(cart, user.getCart());
                }
            }
            cart = cartRepository.save(cart);
            user = userRepository.save(user);
            model.addAttribute("cart", cart);
            model.addAttribute("loggedUser", user);
        }

        CartItem productInCart = cartItemRepository.findFirstByProductAndCart(product, cart).orElse(null);
        boolean isProductInCart = cartItemRepository.findFirstByProductAndCart(product, cart).isPresent();

        if (productInCart != null) {
            productInCart.setQuantity(productInCart.getQuantity() + cartItem.getQuantity());
            cartItem = productInCart;
        }

        cartItem.setCart(cart);
        cartItem.setProduct(product);
        cartItemRepository.save(cartItem);

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

}
