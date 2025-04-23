import {LoginPlugin} from 'zeenom';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    LoginPlugin.echo({value: inputValue}).then(result => {
            console.log("js: " + JSON.stringify(result));
            document.getElementById("echoInput").value = result.value + " ";
            document.getElementById("description").textContent = result.value;
        }
    );
}