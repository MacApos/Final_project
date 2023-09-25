document.addEventListener("DOMContentLoaded", function (event) {
    const session = document.querySelector('#session');
    const logout = document.querySelector('#logout');
    const login = document.querySelector('#login');
    session.setAttribute("hidden", "hidden");

    if(session.innerText===""){
        logout.removeAttribute("hidden")
        login.setAttribute("hidden", "hidden");
        console.log('no logged user');
    } else {
        logout.setAttribute("hidden", "hidden");
        login.removeAttribute("hidden");
        console.log(session.innerText);
        console.log('logged user');
    }

})