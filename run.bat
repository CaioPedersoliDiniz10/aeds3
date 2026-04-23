@echo off
echo === BiblioSystem - Compilando ===

set SRC=src
set BIN=bin

if not exist %BIN% mkdir %BIN%

echo Compilando arquivos Java...
javac -encoding UTF-8 -d %BIN% ^
  %SRC%\model\Usuario.java ^
  %SRC%\model\Livro.java ^
  %SRC%\model\Emprestimo.java ^
  %SRC%\model\EmprestimoItem.java ^
  %SRC%\model\Cupom.java ^
  %SRC%\dao\HashExtensivel.java ^
  %SRC%\dao\ArvoreBPlus.java ^
  %SRC%\dao\OrdenacaoExternaLivro.java ^
  %SRC%\dao\UsuarioDAO.java ^
  %SRC%\dao\LivroDAO.java ^
  %SRC%\dao\EmprestimoDAO.java ^
  %SRC%\dao\EmprestimoItemDAO.java ^
  %SRC%\dao\EmprestimoItemDAOIndexado.java ^
  %SRC%\dao\CupomDAO.java ^
  %SRC%\controller\UsuarioController.java ^
  %SRC%\controller\LivroController.java ^
  %SRC%\controller\CupomController.java ^
  %SRC%\controller\EmprestimoController.java ^
  %SRC%\server\StaticHandler.java ^
  %SRC%\server\UsuarioHandler.java ^
  %SRC%\server\LivroHandler.java ^
  %SRC%\server\CupomHandler.java ^
  %SRC%\server\EmprestimoHandler.java ^
  %SRC%\server\AppServer.java

if %errorlevel% neq 0 (
    echo ERRO na compilacao!
    pause
    exit /b 1
)

echo.
echo === Compilacao concluida! Iniciando servidor... ===
echo Acesse: http://localhost:8080
echo.

java -cp %BIN% server.AppServer

pause
