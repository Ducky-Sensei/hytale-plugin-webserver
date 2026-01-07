document.addEventListener('DOMContentLoaded', () => {
   document.getElementById('loginCodeBtn').addEventListener('click', (e) => {
       e.preventDefault();
       showCardBody('cardLoginCode');
   });

    document.getElementById('loginCodeClose').addEventListener('click', (e) => {
        e.preventDefault();
        showCardBody('cardLoginSelection');
    });

    document.getElementById('loginPasswordBtn').addEventListener('click', (e) => {
        e.preventDefault();
        showCardBody('cardLoginPassword');
    });

    document.getElementById('loginPasswordClose').addEventListener('click', (e) => {
        e.preventDefault();
        showCardBody('cardLoginSelection');
    });

    document.getElementById('loginPasswordCreateBtn').addEventListener('click', (e) => {
        e.preventDefault();
        showCardBody('cardLoginPasswordCreate');
    });

    document.getElementById('loginPasswordCreateClose').addEventListener('click', (e) => {
        e.preventDefault();
        showCardBody('cardLoginSelection');
    });

    function showCardBody(id) {
        document.getElementById('cardLogin').classList.add('hidden');

        (new Promise(resolve => setTimeout(resolve, 200))).then((result) => {
            document.querySelectorAll("#cardLogin .card-body").forEach((element) => {
                element.classList.add("d-none");
            })

            document.getElementById(id).classList.remove('d-none');

            document.getElementById('cardLogin').classList.remove('hidden');
        })
    }

    document.getElementById("passwordCreatePassword").addEventListener('change', validatePasswordFields)
    document.getElementById("passwordCreatePasswordRepeated").addEventListener('change', validatePasswordFields)

    function validatePasswordFields() {
        let valuesMatch = document.getElementById('passwordCreatePassword').value === document.getElementById('passwordCreatePasswordRepeated').value

        let repeatElement = document.getElementById('passwordCreatePasswordRepeated');
        let button = document.querySelector("#cardLoginPasswordCreate button[type=submit]");

        if (valuesMatch) {
            repeatElement.classList.remove('is-invalid');
            button.attributes.disabled = false;
            button.removeAttribute('disabled');
        } else {
            repeatElement.classList.add('is-invalid');
            button.setAttribute('disabled', 'disabled');
        }
    }
});
