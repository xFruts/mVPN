export default function MainAddUsers(){
    return(
        <>
            <div className="addusers-header">
                Добавить пользователся
            </div>
            <div className="addusers-body">
                <input type="text" name="name" placeholder="Введите имя и отчество или никнейм"/>
                <button>Создать пользователя</button>
            </div>
        </>
    );
}