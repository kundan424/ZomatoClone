// Wait for the HTML document to fully load before running the script
document.addEventListener("DOMContentLoaded", function () {

    // Grab every accordion header button on the page
    const accordionHeaders = document.querySelectorAll(".accordion-header");

    accordionHeaders.forEach(header => {
        header.addEventListener("click", function () {

            // Toggle the 'active' class on the clicked button (changes the + to a -)
            this.classList.toggle("active");

            // Find the specific explanation div that lives right below this button
            const content = this.nextElementSibling;

            // Toggle the 'show' class to expand or collapse the text
            if (content.classList.contains("show")) {
                content.classList.remove("show");
            } else {
                content.classList.add("show");
            }
        });
    });
});