<?php

namespace Application\Controller;

use Application\Exception\ValidationException;
use Application\Helper\Message;
use Application\Model\PedidoModel;
use Application\View\ApplicationView;
use Szy\Mvc\Model\ModelException;
use Szy\Util\DateTime;

class PedidoController extends AdminController
{
    /**
     * @var DateTime
     */
    private $data;

    public function init()
    {
        parent::init(); // TODO: Change the autogenerated stub
        $this->data = new DateTime('now');
    }

    public function indexAction()
    {
        $this->abertosAction();
    }

    public function abertosAction()
	{
		$view = new ApplicationView($this, 'pedido/index');
		$view->setTitle('Pedidos abertos');

		$model = new PedidoModel();
		$view->setAttribute('pedidos', $model->pedidos(PedidoModel::$ESTADO_ABERTO));

		$view->flush();
	}

	public function fechadosAction()
	{
        $view = new ApplicationView($this, 'pedido/index');
        $view->setTitle('Pedidos fechados');

        $model = new PedidoModel();
        $view->setAttribute('pedidos', $model->pedidos(PedidoModel::$ESTADO_FECHADO));

        $view->flush();
	}

	public function detalhesAction($codigo)
	{
		$view = new ApplicationView($this, 'pedido/detalhe');
		$model = new PedidoModel();
		try {
			$pedido = $model->row('pedido', null, 'codigo = ?', array($codigo));
			if ($pedido == null)
				throw new ValidationException('Código do pedido inválido');

			$view->setAttribute('pedido', $model->detalhes($codigo));
		} catch (ModelException $ex) {
			$view->setMessage(new Message($ex->getMessage(), Message::TYPE_DANGER));
		}
		$view->flush();
	}

	public function produzirAction($codigo)
	{
		$model = new PedidoModel();
		$pedido = $model->row('pedido', null, 'codigo = ?', array($codigo));
		if ($pedido == null) {
			$this->setSessionMessage(new Message('Código do pedido inválido', Message::TYPE_DANGER));
		} else if ($pedido->situacao == '2') {
			$this->setSessionMessage(new Message('Este pedido já foi finalizado', Message::TYPE_DANGER));
		} else {
			try {
				$arguments = array(
                    'dt_producao' => $this->data->format('Y-m-d H:i:s'),
                    'situacao' => PedidoModel::$ESTADO_PRODUCAO
				);
				$model->update('pedido', $arguments, 'codigo = ?', array($pedido->codigo));

				$this->setSessionMessage(new Message('Iniciado produção do pedido', Message::TYPE_SUCCESS));
			} catch (\Exception $ex) {
				$this->setSessionMessage(new Message($ex->getMessage(), Message::TYPE_DANGER));
			}
		}

		$this->getResponse()->sendRedirect('/pedidos/abertos');
	}

	public function finalizarAction($codigo)
	{
		$model = new PedidoModel();
		$pedido = $model->row('pedido', null, 'codigo = ?', array($codigo));
		if ($pedido == null) {
			$this->setSessionMessage(new Message('Código do pedido inválido', Message::TYPE_DANGER));
		} else if ($pedido->situacao == '1') {
			$this->setSessionMessage(new Message('O pedido precisa ser enviado para produção antes de ser finalizado', Message::TYPE_DANGER));
		} else {
			try {
				$arguments = array(
                    'dt_fechado' => $this->data->format('Y-m-d H:i:s'),
                    'usuario' => $this->usuario->codigo,
					'situacao' => PedidoModel::$ESTADO_FECHADO
				);
				$model->update('pedido', $arguments, 'codigo = ?', array($pedido->codigo));

				$this->setSessionMessage(new Message('Pedido finalizado com sucesso', Message::TYPE_SUCCESS));
			} catch (\Exception $ex) {
				$this->setSessionMessage(new Message($ex->getMessage(), Message::TYPE_DANGER));
			}
		}

		$this->getResponse()->sendRedirect('/pedidos/abertos');
	}
}