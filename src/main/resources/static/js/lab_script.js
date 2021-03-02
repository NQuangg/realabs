let database = firebase.database();
let ref = database.ref('key');

let steps = document.getElementsByClassName('steps')[0].children[0];
steps.addEventListener('click', function(e) {
    let target = e.target;
    if (target.tagName === 'A' && target.getAttribute('href')[0] === '#') {
        let step = parseInt(target.getAttribute('href').slice(1));
        ref.set(step);
    }
}, false);


ref.on('value', (snapshot) => {
    const data = snapshot.val();
    steps.children[data].children[0].click();
});


