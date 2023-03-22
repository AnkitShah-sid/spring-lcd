const drop_down_div = document.querySelector('#drop-down-div-save');
const caret = document.querySelector('#caret-save');
const caretIcon = document.querySelector('#fa-caret-down-save');
const options = document.querySelector('#board-save');
const ps = document.querySelectorAll('#board-save p');
const selectedOption = document.querySelector('#selected-option-save p');

ps.forEach(p => {
  p.addEventListener('mouseover', () => {
    p.classList.add('p-color');
  });

  p.addEventListener('mouseleave', () => {
    p.classList.remove('p-color');
  });
  
  p.addEventListener('click', () => {
    options.classList.toggle('show');
    caretIcon.classList.toggle('caret-down');
    selectedOption.textContent = p.textContent;
  });
});

drop_down_div.addEventListener('click', (e) => {
    options.classList.toggle('show');
    caretIcon.classList.toggle('caret-down');
});