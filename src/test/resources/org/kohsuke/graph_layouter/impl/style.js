window.onload = function() {
    var name = document.getElementById("name");

    function apply(n) {
        n.onmouseover = function() {
            name.innerHTML = this.getAttribute("tag");
        }
        n.onmouseout = function() {
            name.innerHTML = "";
        }
    }

    var nodes = document.querySelectorAll("DIV.node");
    for (var i=0;i<nodes.length;i++) {
        apply(nodes.item(i));
    }
}
