import { NextFunction, Request, Response } from 'express'
import productService from '../product/product.service'
import { CartItem } from './cart-item.entity'
import cartItemService, { CART } from './cart-item.service'

export const list = async (
  _req: Request,
  res: Response,
  _next: NextFunction,
) => {
  res.send(CART)
}

export const add = async (req: Request, res: Response, _next: NextFunction) => {
  const { productId, quantity } = req.body

  const product = await productService.getById(productId)
  if (!product) {
    res.sendStatus(404)
    return
  }

  const newItem: CartItem = {
    product: productId,
    quantity,
  }

  const saved = await cartItemService.add(newItem)

  res.json(saved)
}

export const updateQuantity = async (
  req: Request,
  res: Response,
  _next: NextFunction,
) => {
  const { id, newQuantity } = req.params

  const product = await productService.getById(id)
  if (!product) {
    res.sendStatus(404)
    return
  }

  const updatedItem = await cartItemService.updateQuantity(
    id,
    parseInt(newQuantity),
  )

  res.json(updatedItem)
}

export const remove = async (
  _req: Request,
  _res: Response,
  _next: NextFunction,
) => {}
