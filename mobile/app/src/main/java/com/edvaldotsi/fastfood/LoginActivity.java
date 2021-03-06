package com.edvaldotsi.fastfood;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.edvaldotsi.fastfood.dao.ContaDAO;
import com.edvaldotsi.fastfood.model.Cliente;
import com.edvaldotsi.fastfood.model.Conta;
import com.edvaldotsi.fastfood.request.PostData;
import com.edvaldotsi.fastfood.request.ServerRequest;
import com.edvaldotsi.fastfood.request.ServerResponse;
import com.edvaldotsi.fastfood.validator.EmailValidator;
import com.edvaldotsi.fastfood.validator.EmptyValidator;
import com.rengwuxian.materialedittext.MaterialEditText;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends ToolbarActivity { // Android hash key: TnBF+9F/RrO8gS3wwRe83jgkQcQ=

    private SharedPreferences settings;

    private Cliente cliente;

    private MaterialEditText edtEmail;
    private MaterialEditText edtSenha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setLayout(R.layout.activity_login);
        super.onCreate(savedInstanceState);
        getToolbar().setTitle(getString(R.string.title_activity_login));
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        edtEmail = (MaterialEditText) findViewById(R.id.edtEmail);
        edtEmail.addValidator(new EmailValidator("Endereço de e-mail inválido"));
        edtEmail.setValidateOnFocusLost(true);

        edtSenha = (MaterialEditText) findViewById(R.id.edtSenha);
        edtSenha.addValidator(new EmptyValidator("Preencha o campo senha"));
        edtSenha.setValidateOnFocusLost(true);

        // Verifica se existe email e senha armazenado e faz login automaticamente
        settings = getSharedPreferences("cliente", 0);
        String email = settings.getString("email", "");
        String senha = settings.getString("senha", "");

        if (!"".equals(email) && !"".equals(senha)) {
            requestLogin(email, senha);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            edtEmail.setText(data.getStringExtra("email"));
            edtSenha.setText(data.getStringExtra("senha"));
            btnLoginAction(null);
        }
    }

    public void btnLoginAction(View v) {
        if (!edtEmail.validate() || !edtSenha.validate())
            return;

        String email = edtEmail.getText().toString();
        String senha = ContaDAO.md5(edtSenha.getText().toString());
        requestLogin(email, senha);
    }

    public void btnCadastroAction(View v) {
        Intent intent = new Intent(this, CadastroClienteActivity.class);
        if (edtEmail.validate() && edtSenha.validate()) {
            intent.putExtra("email", edtEmail.getText().toString());
            intent.putExtra("senha", edtSenha.getText().toString());
        }
        startActivityForResult(intent, 100);
    }

    private void requestLogin(String email, String senha) {
        PostData data = new PostData();
        data.put("email", email);
        data.put("senha", senha);

        ServerRequest request = new ServerRequest(this, this, ServerRequest.METHOD_POST);
        request.send("/login", data);
    }

    @Override
    public void onResponseSuccess(ServerResponse response) {
        try {
            if (response.getCode() == 406 || response.getCode() == 400)
                return;

            if (response.getCode() == 200) {
                JSONObject json = response.getJSONObject("cliente");
                cliente = response.decode(json.toString(), Cliente.class);

                // Cria um novo objeto conta e seta o cliente obtido pelo login
                ContaDAO.setConta(new Conta(cliente, null, null));

                SharedPreferences.Editor editor = settings.edit();
                editor.putString("email", cliente.getEmail());
                editor.putString("senha", cliente.getSenha());
                editor.apply();

                edtEmail.setText("");
                edtSenha.setText("");

                startActivity(new Intent(this, MainActivity.class));
                //startActivity(new Intent(this, ClassificarActivity.class));
            }
        } catch (JSONException ex) {
            showMessage("Erro ao fazer login, tente novamente mais tarde");
            Log.e("LOGIN", ex.getMessage());
        }
    }

    @Override
    public void onResponseError(ServerResponse response) {

        if (response.getCode() == 401) {
            showMessage("Login ou senha inválida");
        } else {
            super.onResponseError(response);
        }
    }
}
