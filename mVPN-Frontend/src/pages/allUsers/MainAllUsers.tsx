const Users = [
    {
        id: 1,
        role: "admin",
        name: "Admin",
        key: "046568",
        telegramId: "79673",
        file: ""
    },
    {
        id: 1,
        role: "admin",
        name: "Admin",
        key: "046568",
        telegramId: "79673",
        file: ""
    },
    {
        id: 1,
        role: "admin",
        name: "Admin",
        key: "046568",
        telegramId: "79673",
        file: ""
    }
]

export default function MainAllUsers(){
    return(
        <>
            <div className="allusers-header">
                Список пользователей
            </div>
            <div className="allusers-filter">
                <table className="filter-table">
                    <tbody>
                        <tr>
                            <td>Фильтрация</td>
                            <td>Роль</td>
                            <td><input type="text" name="search" placeholder="Поиск"/></td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div className="allusers-table">
                <table>
                    <tbody>
                        <tr>
                            <td>Роль</td>
                            <td>Ник</td>
                            <td>Ключ</td>
                            <td>Телеграмм</td>
                            <td>Файл</td>
                        </tr>
                        {Users.map(user => (
                            <tr>
                                <td>{user.role}</td>
                                <td>{user.name}</td>
                                <td>{user.key}</td>
                                <td>{user.telegramId}</td>
                                <td>{user.file}</td>
                                <td>Удалить</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </>
    );
}